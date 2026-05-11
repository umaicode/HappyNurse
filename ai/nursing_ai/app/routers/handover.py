import time
from fastapi import APIRouter, Depends, Query, HTTPException
from sse_starlette.sse import EventSourceResponse
from app.middleware.jwt_auth import get_current_user

router = APIRouter()

_roster_cache: dict[str, dict] = {}
_CACHE_TTL = 1800  # 30분

_pipeline = None
_roster_service = None
_session_factory = None
_record_loader = None
_rule_engine = None
_lexicon = None
_llm = None
_persister = None
_settings_meta = None
_job_coordinator = None
_freshness_repo = None


def configure(*, pipeline, roster_service, session_factory, record_loader, rule_engine,
            lexicon, llm, persister, settings_meta, job_coordinator, freshness_repo):
    global _pipeline, _roster_service, _session_factory, _record_loader, _rule_engine
    global _lexicon, _llm, _persister, _settings_meta, _job_coordinator, _freshness_repo
    _pipeline = pipeline; _roster_service = roster_service; _session_factory = session_factory
    _record_loader = record_loader; _rule_engine = rule_engine; _lexicon = lexicon
    _llm = llm; _persister = persister; _settings_meta = settings_meta
    _job_coordinator = job_coordinator; _freshness_repo = freshness_repo


@router.post("/generate",
             summary="인수인계 리포트 일괄 생성",
             description="담당 환자 전체의 인수인계 리포트를 비동기로 생성합니다. 반환된 job_id로 진행 상황을 스트리밍 조회하세요.")
async def generate(current_user: dict = Depends(get_current_user)):
    practitioner_id = str(current_user["practitioner_id"])
    # 새 리포트 생성 시 Roster Summary 캐시 무효화
    _roster_cache.pop(practitioner_id, None)
    encounters = await _roster_service.list_for_practitioner(practitioner_id)
    job_id = _job_coordinator.create_job(encounters)
    import asyncio
    asyncio.create_task(_run_job(job_id, practitioner_id, encounters))
    return {"job_id": job_id}


async def _run_job(job_id: str, practitioner_id: str, encounter_ids: list[str]):
    from datetime import datetime, timezone, timedelta
    from app.services.handover.coordination.roster_summary import assemble_roster
    now = datetime.now(timezone.utc)
    shift = (now - timedelta(hours=8), now)
    prev = (now - timedelta(hours=16), now - timedelta(hours=8))

    async def one(enc_id: str):
        await _job_coordinator.emit_progress(job_id, enc_id, "started")
        async with _session_factory() as session:
            try:
                from app.services.handover.processing.pipeline import generate_for_encounter
                res = await generate_for_encounter(
                    encounter_id=enc_id, from_practitioner_id=practitioner_id,
                    shift_window=shift, prev_shift_window=prev,
                    record_loader=_record_loader, rule_engine=_rule_engine, lexicon=_lexicon,
                    llm=_llm, session=session, persister=_persister, settings_meta=_settings_meta,
                )
                await session.commit()
                vs = {"ok": 0, "partial": 0, "failed": 0}
                for s in res.payload["slots"].values():
                    vs[s["verification"]] = vs.get(s["verification"], 0) + 1
                await _job_coordinator.emit_complete(job_id, enc_id,
                                                    handover_id=res.handover_id,
                                                    verification_summary=vs)
                return {"encounter_id": enc_id, "handover_id": res.handover_id,
                        "json": res.payload, "freshness": {"new_records_since_report": 0}}
            except Exception as e:
                await _job_coordinator.emit_error(job_id, enc_id, reason=str(e), fallback_handover_id=None)
                return None

    import asyncio
    results = await asyncio.gather(*[one(e) for e in encounter_ids])
    handovers = [r for r in results if r]
    if handovers:
        roster = await assemble_roster(patient_handovers=handovers, llm=_llm,
                                        model_name=_settings_meta["model"])
        await _job_coordinator.emit_roster_summary(job_id, payload=roster)
    await _job_coordinator.emit_job_done(job_id)


@router.get("/stream/{job_id}",
            summary="인수인계 생성 SSE 스트림",
            description="⚠️ Swagger UI에서 직접 실행 시 연결이 유지됩니다. curl 또는 프론트엔드에서 사용하세요.")
async def stream(job_id: str, current_user: dict = Depends(get_current_user)):
    async def gen():
        import json
        async for evt in _job_coordinator.stream(job_id):
            yield {"event": evt["event"], "data": json.dumps(evt["data"])}
    return EventSourceResponse(gen())


@router.get("/{handover_id}/freshness",
            summary="리포트 생성 이후 신규 간호기록 수 조회",
            description="Briefing 모드에서 리포트가 최신인지 확인할 때 사용합니다.")
async def freshness(handover_id: str, current_user: dict = Depends(get_current_user)):
    return await _freshness_repo.count_new_records_since_report(handover_id)


@router.get("/roster-summary",
            summary="담당 환자 전체 시프트 요약 즉석 조립",
            description="Briefing 진입 시 담당 환자 전체의 요약을 즉석으로 생성합니다. DB에 저장되지 않습니다.")
async def roster_summary(current_user: dict = Depends(get_current_user)):
    from sqlalchemy import select, desc
    from app.services.handover.db.models import ShiftHandover
    from app.services.handover.coordination.roster_summary import assemble_roster
    practitioner_id = str(current_user["practitioner_id"])

    # 캐시 확인 — 30분 내 동일 간호사의 요청은 LLM 재호출 없이 반환
    cached = _roster_cache.get(practitioner_id)
    if cached and time.time() < cached["expires_at"]:
        return cached["data"]

    encounters = await _roster_service.list_for_practitioner(practitioner_id)
    handovers = []
    async with _session_factory() as session:
        for enc in encounters:
            r = await session.execute(
                select(ShiftHandover).where(ShiftHandover.encounter_id == int(enc))
                .order_by(desc(ShiftHandover.created_at)).limit(1)
            )
            row = r.scalar_one_or_none()
            if row:
                fresh = await _freshness_repo.count_new_records_since_report(str(row.handover_id))
                handovers.append({"encounter_id": enc, "handover_id": str(row.handover_id),
                                    "json": row.auto_summary_json, "freshness": fresh})
    if not handovers:
        return {"narrative_header": "", "stats": {"patient_count": 0}, "patients": [],
                "verification_followup": [], "meta": {"model_for_narrative_only": _settings_meta["model"]}}

    result = await assemble_roster(patient_handovers=handovers, llm=_llm, model_name=_settings_meta["model"])

    # 캐시 저장 — practitioner_id별 분리 저장
    _roster_cache[practitioner_id] = {
        "data": result,
        "expires_at": time.time() + _CACHE_TTL,
    }
    return result


@router.get("",
            summary="특정 환자 최신 인수인계 리포트 조회",
            description="encounter_id로 해당 환자의 가장 최근 인수인계 리포트를 조회합니다.")
async def get_latest_for_encounter(
    encounter_id: str = Query(..., description="입원 ID"),
    latest: bool = Query(True, description="최신 1건만 조회 여부"),
    current_user: dict = Depends(get_current_user),
):
    from sqlalchemy import select, desc
    from app.services.handover.db.models import ShiftHandover
    async with _session_factory() as session:
        stmt = select(ShiftHandover).where(
            ShiftHandover.encounter_id == int(encounter_id)
        ).order_by(desc(ShiftHandover.created_at))
        if latest:
            stmt = stmt.limit(1)
        result = await session.execute(stmt)
        rows = result.scalars().all()
        if not rows:
            raise HTTPException(status_code=404)
        return [{"handover_id": str(r.handover_id), "auto_summary": r.auto_summary,
                "auto_summary_json": r.auto_summary_json,
                "created_at": r.created_at.isoformat()} for r in rows]


@router.get("/{handover_id}",
            summary="인수인계 리포트 단건 조회",
            description="handover_id로 특정 인수인계 리포트를 조회합니다.")
async def get_one(handover_id: str, current_user: dict = Depends(get_current_user)):
    from app.services.handover.db.models import ShiftHandover
    async with _session_factory() as session:
        row = await session.get(ShiftHandover, int(handover_id))
        if not row:
            raise HTTPException(status_code=404)
        return {"handover_id": str(row.handover_id), "encounter_id": str(row.encounter_id),
                "auto_summary": row.auto_summary, "auto_summary_json": row.auto_summary_json,
                "created_at": row.created_at.isoformat()}


@router.post("/{encounter_id}/regenerate",
             summary="단일 환자 인수인계 재생성",
             description="특정 환자의 인수인계 리포트를 새로 생성합니다. 기존 리포트는 유지됩니다.")
async def regenerate(encounter_id: str, current_user: dict = Depends(get_current_user)):
    practitioner_id = str(current_user["practitioner_id"])
    # 재생성 시 Roster Summary 캐시 무효화
    _roster_cache.pop(practitioner_id, None)
    job_id = _job_coordinator.create_job([encounter_id])
    import asyncio
    asyncio.create_task(_run_job(job_id, practitioner_id, [encounter_id]))
    return {"job_id": job_id}