from datetime import datetime, timezone
from app.services.handover.prompts.roster_summary import SYSTEM, USER_TEMPLATE


_SEVERITY_SCORE = {"high": 5, "medium": 3, "low": 1}


def _risk_score(h: dict) -> int:
    payload = h["json"]
    sev = sum(_SEVERITY_SCORE.get(r["severity"], 0) for r in payload.get("rules_fired", []))
    partials = sum(1 for s in payload["slots"].values() if s["verification"] != "ok")
    return sev + partials


def deterministic_aggregate(handovers: list[dict]) -> dict:
    patients = []
    followup = []
    for h in handovers:
        payload = h["json"]
        verification_summary = {"ok": 0, "partial": 0, "failed": 0}
        for s in payload["slots"].values():
            v = s["verification"]
            verification_summary[v] = verification_summary.get(v, 0) + 1
        item = {
            "encounter_id": h["encounter_id"],
            "handover_id": h["handover_id"],
            "header": payload["header"],
            "risk_score": _risk_score(h),
            "rules_fired_brief": [r["label"] for r in payload.get("rules_fired", [])],
            "verification_summary": verification_summary,
            "freshness": h.get("freshness", {"new_records_since_report": 0}),
        }
        patients.append(item)
        for slot_name, slot in payload["slots"].items():
            if slot["verification"] != "ok":
                followup.append({
                    "encounter_id": h["encounter_id"],
                    "slot": slot_name,
                    "reason": "검증 미통과 항목 존재",
                })
    patients.sort(key=lambda p: p["risk_score"], reverse=True)
    return {
        "patients": patients,
        "verification_followup": followup,
        "stats": {"patient_count": len(patients)},
    }


async def assemble_roster(*, patient_handovers: list[dict], llm, model_name: str) -> dict:
    agg = deterministic_aggregate(patient_handovers)
    patients_block = "\n".join(
        f"- [{p['encounter_id']}] {p['header']}"
        + (f"  / 룰: {', '.join(p['rules_fired_brief'])}" if p["rules_fired_brief"] else "")
        for p in agg["patients"]
    )
    narrative = {"narrative_header": ""}
    if patients_block:
        narrative = await llm.chat_json(
            system=SYSTEM,
            user=USER_TEMPLATE.format(patients_block=patients_block),
            json_schema={
                "type": "object",
                "properties": {"narrative_header": {"type": "string"}},
                "required": ["narrative_header"],
                "additionalProperties": False,
            },
            temperature=0.0,
        )
    return {
        "schema_version": "1.0",
        "kind": "roster_summary",
        "narrative_header": narrative["narrative_header"],
        "stats": agg["stats"],
        "patients": agg["patients"],
        "verification_followup": agg["verification_followup"],
        "meta": {
            "model_for_narrative_only": model_name,
            "assembled_at": datetime.now(timezone.utc).isoformat(),
            "source_handovers": [h["handover_id"] for h in patient_handovers],
        },
    }