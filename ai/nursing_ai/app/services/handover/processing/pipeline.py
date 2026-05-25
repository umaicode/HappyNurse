import json
import logging
import re
from dataclasses import dataclass
from datetime import datetime, timezone

from app.services.handover.processing.extractor import extract_handover, deterministic_vitals
from app.services.handover.clinical.illness_severity import calculate_severity
from app.services.handover.clients.iv_client import calculate_iv_remaining
from app.services.handover.processing.verifier import enforce_citation, apply_lint
from app.services.handover.processing.renderer import render_markdown
from app.services.handover.clinical.rule_trigger import should_fetch_tier3
from app.services.handover.clinical.record_loader import RecordLoader, LoadedContext
from app.services.handover.processing.phi_tokenizer import PHITokenizer
# KHNA 통합 모듈
from app.services.handover.clinical.profile_parser import parse_profile
from app.services.handover.clinical.scale_evaluator import (
    compute_news2, compute_qsofa,
)
from app.services.handover.clinical.khna_classifier import classify_score
from app.services.handover.clinical.isolation_mapper import detect_isolation_requirements
from app.services.handover.clinical.medication_classifier import compute_medication_fall_risk_score
from app.services.handover.clinical.postop_timeline import suggest_complication_categories
from app.services.handover.clinical.injury_risk_evaluator import evaluate_injury_risk
from app.services.handover.clinical.patient_categorizer import categorize_patient, applicable_bundles

logger = logging.getLogger(__name__)


@dataclass
class HandoverResult:
    handover_id: str
    text: str
    payload: dict


async def generate_for_encounter(
    *,
    encounter_id: str,
    from_practitioner_id: str,
    shift_window: tuple[datetime, datetime],
    prev_shift_window: tuple[datetime, datetime],
    record_loader: RecordLoader,
    rule_engine,
    lexicon,
    llm,
    session,
    persister,
    settings_meta: dict,
    identifier_fields: dict[str, str] | None = None,
    iv_client=None,
) -> HandoverResult:
    s_start, s_end = shift_window
    ctx = await record_loader.load(encounter_id=encounter_id, shift_start=s_start, shift_end=s_end)

    decision = should_fetch_tier3(rule_engine, ctx={
        "pod": ctx.tier2.pod, "hd": ctx.tier2.hd,
        "last_shift_text": ctx.tier1_text, "last_shift_events": [],
    })
    if decision.fetch:
        ctx = await record_loader.extend_with_tier3(
            ctx, encounter_id=encounter_id,
            prev_shift_start=prev_shift_window[0], prev_shift_end=prev_shift_window[1])

    tokenizer = PHITokenizer()
    fields = identifier_fields or {}
    masked_tier1, m1 = tokenizer.mask(ctx.tier1_text, fields)
    masked_tier3, m3 = tokenizer.mask(ctx.tier3_text, fields)
    mapping = {**m1, **m3}

    tier2_text = (
        f"입원진단: {ctx.tier2.admission_dx}, 수술: {ctx.tier2.surgery}, "
        f"POD={ctx.tier2.pod}, HD={ctx.tier2.hd}, "
        f"알레르기: {ctx.tier2.allergies}, 격리: {ctx.tier2.isolation}, "
        f"DNR: {ctx.tier2.dnr}, 고위험약물: {','.join(ctx.tier2.high_alert_meds) or '없음'}"
    )
    masked_tier2, m2 = tokenizer.mask(tier2_text, fields)
    mapping.update(m2)

    # 환자 컨디션 자동 분류 — LLM prompt 컨디션 주입용
    # (documentation_standards/bundle_care 자산 활용)
    try:
        patient_types = categorize_patient(
            tier2=ctx.tier2,
            nursing_record_text=ctx.tier1_text + "\n" + ctx.tier3_text,
        )
        bundle_ids = applicable_bundles(patient_types)
    except Exception as e:
        logger.warning("환자 분류 실패 [encounter=%s]: %s", encounter_id, e)
        patient_types, bundle_ids = [], []

    try:
        raw = await extract_handover(
            tier1=masked_tier1, tier2=masked_tier2, tier3=masked_tier3,
            llm=llm, lexicon=lexicon,
            patient_types=patient_types, bundle_ids=bundle_ids)
        failures = []
    except Exception as e:
        logger.error("LLM 추출 실패 [encounter=%s]: %s", encounter_id, e, exc_info=True)
        empty_failed = lambda: {"items": [], "verification": "failed"}
        raw = {
            "header": "(LLM 실패: 면대면 인계에서 원문 확인 필요)",
            "slots": {k: empty_failed() for k in ["patient_problem", "assessment", "situation", "safety",
"background", "action", "recommendation", "synthesis"]},
            "citations": [{"id": "raw-1", "record_id": "tier1", "line_range": [1, 1],
                            "ts": (ctx.last_record_ts.isoformat() if ctx.last_record_ts else
datetime.now(timezone.utc).isoformat()),
                            "label": "원문 (LLM 실패 폴백)"}],
        }
        for v in deterministic_vitals(ctx.tier1_text):
            if v["kind"] == "bp":
                val_str = f"BP {v['value']['sys']}/{v['value']['dia']}"
            elif v["kind"] == "hr":
                val_str = f"HR {v['value']}"
            elif v["kind"] == "spo2":
                val_str = f"SpO2 {v['value']}%"
            elif v["kind"] == "temp":
                val_str = f"Temp {v['value']}"
            else:
                val_str = str(v["value"])
            raw["slots"]["assessment"]["items"].append({
                "kind": v["kind"], "value": val_str,
                "citation_ids": ["raw-1"], "source_layer": 1, "confidence": 0.9,
            })
        if raw["slots"]["assessment"]["items"]:
            raw["slots"]["assessment"]["verification"] = "partial"
        failures = [{"slot": "*", "reason": f"llm_extract_failed: {e}"}]

    raw_str = json.dumps(raw, ensure_ascii=False)
    raw = json.loads(tokenizer.unmask(raw_str, mapping))

    if hasattr(record_loader, 'load_nfc_meds'):
        nfc_meds = await record_loader.load_nfc_meds(
            encounter_id=encounter_id,
            shift_start=s_start, shift_end=s_end
        )
        for med in nfc_meds:
            citation_id = f"nfc-{med.admin_id}"
            raw["citations"] = raw.get("citations", [])
            raw["citations"].append({
                "id": citation_id,
                "record_id": med.admin_id,
                "line_range": [1, 1],
                "ts": med.administered_at.isoformat(),
                "label": f"NFC투약: {med.drug_name}",
            })
            raw["slots"]["action"]["items"].insert(0, {
                "kind": "medication",
                "value": f"{med.drug_name} {med.dose} {med.route} ({med.administered_at.strftime('%H:%M')})",
                "citation_ids": [citation_id],
                "source_layer": 1,
                "confidence": 1.0,
            })

    record_text_by_id = {
        r.record_id: r.text
        for r in (ctx.tier1_records + ctx.tier3_records)
    }
    for c in raw.get("citations", []):
        rec_text = record_text_by_id.get(c.get("record_id"))
        line_range = c.get("line_range") or []
        if not rec_text or not line_range:
            continue
        lines = rec_text.splitlines()
        start_1 = max(1, int(line_range[0]))
        end_1 = int(line_range[-1]) if len(line_range) >= 2 else start_1
        if end_1 < start_1:
            end_1 = start_1
        snippet = "\n".join(lines[start_1 - 1:end_1]).strip()
        if snippet:
            c["excerpt"] = snippet

    vitals_for_severity = _vitals_from_text(ctx.tier1_text)
    raw["illness_severity"] = calculate_severity(vitals_for_severity).value

    raw["slots"] = enforce_citation(raw["slots"])
    raw["slots"] = apply_lint(raw["slots"], lexicon.inference_blocklist())

    if iv_client is not None:
        try:
            active_ivs = await iv_client.get_active_infusions(encounter_id)
            for iv in active_ivs:
                remaining = calculate_iv_remaining(iv, as_of=s_end)
                if remaining is None:
                    continue
                end_str = remaining.expected_end.strftime("%H:%M")
                raw["slots"]["situation"]["items"].append({
                    "kind": "iv_status",
                    "value": (
                        f"{iv.medication_label} — "
                        f"잔여 약 {remaining.remaining_ml:.0f}mL "
                        f"({end_str} 종료 예정)"
                    ),
                    "citation_ids": [],
                    "source_layer": 4,
                    "confidence": 1.0,
                })
                if remaining.ends_in_shift:
                    raw["slots"]["action"]["items"].insert(0, {
                        "kind": "iv_expiry",
                        "value": f"{iv.medication_label} IV 만료 예정 ({end_str} 종료) — 다음 백 준비 또는 라인 제거 확인",
                        "citation_ids": [],
                        "source_layer": 4,
                        "confidence": 1.0,
                        "time_window": end_str,
                    })
        except Exception as e:
            logger.warning("IV Layer-4 주입 실패 [encounter=%s]: %s", encounter_id, e)

    vitals = {"vitals": _vitals_from_text(ctx.tier1_text)}
    fired = rule_engine.evaluate_clinical(vitals)
    raw.setdefault("rules_fired", []).extend(fired)

    # ───── KHNA 통합: profile_parser + scale_evaluator + classifier 모듈 ─────
    try:
        full_text = ctx.tier1_text + "\n" + ctx.tier3_text
        profile = parse_profile(full_text)
        scores: list[dict] = []
        latest_v = profile.vitals_series[-1] if profile.vitals_series else None

        # NEWS2 (활력 있으면 계산)
        if latest_v:
            n2 = compute_news2(
                rr=latest_v.rr, spo2=latest_v.spo2, on_oxygen=False,
                temp=latest_v.temp, bp_sys=latest_v.bp_sys, hr=latest_v.hr,
                alert=True,
            )
            cls_n2 = classify_score("news2", n2.score)
            scores.append({
                "scale_id": "news2", "score": n2.score,
                "classification": n2.classification, "sources": n2.sources,
                "incomplete": n2.incomplete, "missing_inputs": n2.missing_inputs,
                "details": {"component_scores": n2.component_scores},
                "ui_color": cls_n2.ui_color, "ui_severity": cls_n2.ui_severity,
            })
            # qSOFA
            q = compute_qsofa(
                rr=latest_v.rr, bp_sys=latest_v.bp_sys, altered_mental_status=False,
            )
            cls_q = classify_score("qsofa", q.score)
            scores.append({
                "scale_id": "qsofa", "score": q.score,
                "classification": q.classification, "sources": q.sources,
                "ui_color": cls_q.ui_color, "ui_severity": cls_q.ui_severity,
                "details": {"action": cls_q.action},
            })

        raw["scores"] = scores

        # 임상 스코어를 I-PASS-BAR 슬롯에 통합 — severity_flag로 위험 배너·카운트 연동
        # NEWS2/qSOFA → assessment (활력징후 수치 기반 결정론적 계산만)
        _score_slot = {
            "news2": "assessment", "qsofa": "assessment",
        }
        _score_src = {
            "news2": "RCP NEWS2 2017", "qsofa": "Sepsis-3 2016",
        }
        for s in scores:
            sid = s.get("scale_id", "")
            slot_key = _score_slot.get(sid, "assessment")
            label = _score_src.get(sid, "")
            inc = (f" ⚠️미완(누락:{','.join(s.get('missing_inputs', []))})"
                   if s.get("incomplete") else "")
            src = f" (출처: {label})" if label else ""
            raw["slots"][slot_key]["items"].append({
                "kind": "clinical_score",
                "value": f"{sid.upper()} {s.get('score')}점 — {s.get('classification', '')}{inc}{src}",
                "citation_ids": [],
                "source_layer": 2,
                "confidence": 1.0,
                "severity_flag": s.get("ui_severity"),
            })

        # 격리 자동 매핑 — safety 슬롯에 추가
        for det in detect_isolation_requirements(full_text):
            raw["slots"]["safety"]["items"].append({
                "kind": "isolation_detected",
                "value": f"{det.isolation_type} ({det.microbe_name}) — PPE: {', '.join(det.ppe_required)}",
                "citation_ids": [],
                "source_layer": 2,
                "confidence": 1.0,
                "severity_flag": "watcher",
            })

        # 손상고위험 ABCs (KHNA 낙상관리 III-1.15) — Age·Bone·Coagulation·Surgery
        # nursing_record + medication + 과거력 + surgery를 종합한 위험 프로파일링
        injury_eval = evaluate_injury_risk(
            age=ctx.tier2.age,
            nursing_record_text=full_text,
            surgery_name=ctx.tier2.surgery,
            admission_dx=ctx.tier2.admission_dx,
            history_ids=profile.history_ids,
            medications=ctx.tier2.high_alert_meds,
        )
        if injury_eval.factors:
            factor_summaries = [
                f"{f.label}({', '.join(f.conditions_matched[:2])})"
                for f in injury_eval.factors
            ]
            raw["slots"]["safety"]["items"].append({
                "kind": "injury_high_risk_abcs",
                "value": (
                    f"손상 고위험 — {' + '.join(factor_summaries)} → "
                    f"낙상·외상 시 심각도 증가"
                ),
                "citation_ids": [],
                "source_layer": 2,
                "confidence": 1.0,
                "severity_flag": injury_eval.severity_flag,
            })

        # 5W's 수술 후 합병증 후보 (POD + 발열 시)
        has_fever = bool(latest_v and latest_v.temp and latest_v.temp >= 38.0)
        if profile.pod and has_fever:
            for s in suggest_complication_categories(
                pod=profile.pod, has_fever=True, symptom_text=full_text,
            ):
                cand_str = ", ".join(s["candidates"])
                matched = f" (관찰소견: {', '.join(s['matched_findings'])})" if s["matched_findings"] else ""
                raw["slots"]["assessment"]["items"].append({
                    "kind": "postop_timeline_category",
                    "value": f"{s['pod_range']} {s['category']} 분류 — 후보: {cand_str}{matched}",
                    "citation_ids": [],
                    "source_layer": 2,
                    "confidence": 1.0,
                })
    except Exception as e:
        logger.warning("KHNA 통합 모듈 처리 실패 [encounter=%s]: %s", encounter_id, e)
        raw.setdefault("scores", [])

    raw["slots"] = _dedup_across_slots(raw["slots"])
    raw["slots"] = _prioritize_and_cap(raw["slots"], raw.get("citations", []))

    raw["meta"] = {
        "model": settings_meta["model"],
        "lexicon_version": settings_meta["lexicon_version"],
        "rule_set_version": settings_meta["rule_set_version"],
        "context_tiers_used": ctx.tiers_used,
        "last_record_ts": ctx.last_record_ts.isoformat() if ctx.last_record_ts else None,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "token_usage": {},
        "verifier": {},
        "failures": failures,
    }
    text = render_markdown(raw)

    saved = await persister.save(
        session=session, encounter_id=encounter_id,
        from_practitioner_id=from_practitioner_id,
        text=text, payload=raw,
    )
    return HandoverResult(handover_id=str(saved.handover_id), text=text, payload=raw)


_SLOT_CAPS = {
    "patient_problem": 3,
    "assessment": 4,
    "situation": 3,
    "safety": 5,
    "background": 3,
    "action": 5,
    "recommendation": 3,
    # synthesis: 상한 없음 — 프롬프트가 3~5건 선별 보장
}

# 위험도 순위 (작을수록 우선 보존)
_SEV_RANK = {"unstable": 0, "watcher": 1}

# 결정론적 위험 신호 — 동급에서 우선 보존 (KHNA 스코어·격리·손상고위험·약물·IV)
_PROTECTED_KINDS = {
    "clinical_score", "isolation_detected", "injury_high_risk_abcs",
    "iv_status", "iv_expiry", "medication",
}


def _parse_epoch(s) -> float | None:
    if not s:
        return None
    try:
        dt = datetime.fromisoformat(s)
        if dt.tzinfo is not None:
            dt = dt.replace(tzinfo=None)
        return dt.timestamp()
    except Exception:
        return None


def _prioritize_and_cap(slots: dict, citations: list[dict]) -> dict:
    """슬롯별 우선순위 정렬 후 상한 적용 — 인계를 요약 분량으로.

    우선순위: ①위험도(unstable>watcher) ②결정론적 신호 ③시간지정 처치
    ④최신 사실(동순위 tie-breaker, citation ts 역순) ⑤원래 순서.
    상한 초과분은 조용히 제외 — 인계는 핵심 선별이 본질이므로 별도 표기 안 함.
    """
    cite_ts: dict[str, float] = {}
    for c in citations or []:
        ts = _parse_epoch(c.get("ts"))
        if ts is not None and c.get("id"):
            cite_ts[c["id"]] = ts

    for key, cap in _SLOT_CAPS.items():
        slot = slots.get(key)
        if not slot or not slot.get("items"):
            continue
        items = slot["items"]
        if len(items) <= cap:
            continue

        def sort_key(pair):
            idx, it = pair
            sev = _SEV_RANK.get(it.get("severity_flag"), 2)
            protected = 0 if it.get("kind") in _PROTECTED_KINDS else 1
            has_tw = 0 if it.get("time_window") else 1
            ids = it.get("citation_ids") or []
            ts_vals = [cite_ts[c] for c in ids if c in cite_ts]
            neg_ts = -max(ts_vals) if ts_vals else 0.0
            return (sev, protected, has_tw, neg_ts, idx)

        ranked = sorted(enumerate(items), key=sort_key)
        slot["items"] = [it for _, it in ranked[:cap]]
    return slots


# 슬롯 간 중복 제거 대상 (synthesis 체크리스트는 핵심 재진술이 목적이라 제외)
_DEDUP_SLOT_PRIORITY = [
    "safety", "recommendation", "action", "assessment",
    "situation", "patient_problem", "background",
]


def _norm_tokens(text: str) -> set[str]:
    return set(re.findall(r"[가-힣A-Za-z0-9]+", (text or "").lower()))


def _dedup_across_slots(slots: dict) -> dict:
    """우선순위 높은 슬롯부터 훑어, 같은 인용을 공유하면서 토큰이 다수 겹치는
    항목을 후순위 슬롯에서 제거. (조건부 PRN 등이 평가·조치·권고에 중복되는 문제 완화)

    판정: citation 공유 AND 토큰 60% 이상 겹침 → 중복. synthesis 는 제외.
    인용이 없는 항목(IV 등, citation_ids=[])은 교집합이 비어 절대 제거되지 않음 — 보수적.
    """
    seen: list[tuple[set[str], set[str]]] = []  # (토큰셋, citation셋)
    for key in _DEDUP_SLOT_PRIORITY:
        slot = slots.get(key)
        if not slot or not slot.get("items"):
            continue
        kept = []
        for it in slot["items"]:
            toks = _norm_tokens(it.get("value") or it.get("quote") or "")
            cites = set(it.get("citation_ids") or [])
            is_dup = any(
                (cites & p_cites) and toks and len(toks & p_toks) / len(toks) >= 0.6
                for p_toks, p_cites in seen
            )
            if is_dup:
                continue
            kept.append(it)
            seen.append((toks, cites))
        slot["items"] = kept
    return slots


def _vitals_from_text(text: str) -> dict:
    items = deterministic_vitals(text)
    out = {}
    for it in items:
        if it["kind"] == "bp":
            out["bp_sys"] = it["value"]["sys"]
            out["bp_dia"] = it["value"]["dia"]
        elif it["kind"] == "spo2":
            out["spo2"] = it["value"]
        elif it["kind"] == "temp":
            out["temp"] = it["value"]
        elif it["kind"] == "hr":
            out["hr"] = it["value"]
    return out
