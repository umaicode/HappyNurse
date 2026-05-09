import json
import logging
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

    try:
        raw = await extract_handover(
            tier1=masked_tier1, tier2=masked_tier2, tier3=masked_tier3,
            llm=llm, lexicon=lexicon)
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
