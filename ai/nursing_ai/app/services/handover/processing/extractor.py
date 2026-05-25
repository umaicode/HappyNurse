import re

from app.services.handover.lexicon.store import LexiconStore

_BP = re.compile(r"(?:(?P<time>\d{1,2}:\d{2})\s*)?BP\s*(?P<sys>\d{2,3})\s*/\s*(?P<dia>\d{2,3})", re.IGNORECASE)
_HR = re.compile(r"(?:(?P<time>\d{1,2}:\d{2})\s*)?HR\s*(?P<v>\d{2,3})", re.IGNORECASE)
_SPO2 = re.compile(r"(?:(?P<time>\d{1,2}:\d{2})\s*)?SpO2\s*(?P<v>\d{2,3})\s*%?", re.IGNORECASE)
_TEMP = re.compile(r"(?:(?P<time>\d{1,2}:\d{2})\s*)?Temp\s*(?P<v>\d{2,3}(?:\.\d+)?)", re.IGNORECASE)


def deterministic_vitals(text: str) -> list[dict]:
    out = []
    for m in _BP.finditer(text):
        out.append({"kind": "bp", "time": m.group("time"),
                    "value": {"sys": int(m.group("sys")), "dia": int(m.group("dia"))}})
    for m in _HR.finditer(text):
        out.append({"kind": "hr", "time": m.group("time"), "value": int(m.group("v"))})
    for m in _SPO2.finditer(text):
        out.append({"kind": "spo2", "time": m.group("time"), "value": int(m.group("v"))})
    for m in _TEMP.finditer(text):
        out.append({"kind": "temp", "time": m.group("time"), "value": float(m.group("v"))})
    return out


def categorical_symptoms(text: str, lex: LexiconStore) -> list[dict]:
    out = []
    seen = set()
    for tok in text.replace(",", " ").split():
        gid = lex.match_symptom_group(tok)
        if gid and gid not in seen:
            seen.add(gid)
            out.append({"group_id": gid, "matched_token": tok})
    return out


from app.services.handover.prompts.extractor import SYSTEM, USER_TEMPLATE
from app.services.handover.schemas import HandoverPayload

_EXTRACTION_SCHEMA = HandoverPayload.model_json_schema()
_LLM_SCHEMA = {**_EXTRACTION_SCHEMA}
_LLM_SCHEMA["required"] = [r for r in _EXTRACTION_SCHEMA.get("required", []) if r not in {"meta", "rules_fired",
"illness_severity"}]
_LLM_SCHEMA["properties"] = {k: v for k, v in _EXTRACTION_SCHEMA["properties"].items() if k not in {"meta",
"rules_fired", "illness_severity"}}
_EXCLUDED_DEFS = {"Meta", "RuleFired"}
if "$defs" in _LLM_SCHEMA:
    _LLM_SCHEMA["$defs"] = {k: v for k, v in _LLM_SCHEMA["$defs"].items() if k not in _EXCLUDED_DEFS}


def _format_clinical_scales(scales_data: dict) -> str:
    """assessment_scales.yml → LLM prompt 텍스트 (추론 권고 제외 후)."""
    lines = []
    for s in scales_data.get("scales", []):
        sid = s.get("id", "?")
        name = s.get("name", "")
        rng = f" (범위 {s['range']})" if s.get("range") else ""
        head = f"- **{sid}** ({name}){rng}"
        lines.append(head)
        # thresholds 간단 표기
        thresholds = s.get("thresholds") or []
        for t in thresholds[:6]:
            if not isinstance(t, dict):
                continue
            score_or_range = t.get("score") or t.get("range") or ""
            label = t.get("label") or t.get("risk") or t.get("level") or t.get("interpretation") or ""
            if score_or_range and label:
                lines.append(f"    · {score_or_range}: {label}")
    return "\n".join(lines) if lines else "(척도 정의 없음)"


def _format_documentation_focus(khna_data: dict, patient_types: list[str]) -> str:
    """documentation_standards.yml → 환자 타입에 해당하는 표준 사정 항목만 LLM에."""
    if not patient_types:
        return "(해당 condition 없음)"
    ds = (khna_data.get("documentation_standards") or {}).get("required_assessments_by_condition", {})
    if not ds:
        return "(documentation_standards 데이터 로드 실패)"
    lines = []
    for cond in patient_types:
        spec = ds.get(cond)
        if not spec:
            continue
        lines.append(f"- **{cond}**:")
        for phase_key in ("initial", "initial_24h", "per_shift", "per_episode",
                          "per_shift_minimum", "daily", "pre_suction", "post_suction"):
            items = spec.get(phase_key)
            if not items:
                continue
            if isinstance(items, list):
                lines.append(f"    · {phase_key}: {', '.join(items[:8])}")
    return "\n".join(lines) if lines else "(해당 표준 없음)"


def _format_bundle_vocabulary(khna_data: dict, bundle_ids: list[str]) -> str:
    """bundle_care_checklists.yml → 해당 번들 항목 어휘만 LLM에."""
    if not bundle_ids:
        return "(해당 번들 없음)"
    bundles = (khna_data.get("bundle_care_checklists") or {}).get("bundles", {})
    if not bundles:
        return "(bundle_care_checklists 데이터 로드 실패)"
    lines = []
    for bid in bundle_ids:
        b = bundles.get(bid)
        if not b:
            continue
        lines.append(f"- **{b.get('name_ko', bid)}**:")
        for item in b.get("items", [])[:10]:
            label = item.get("label") if isinstance(item, dict) else str(item)
            if label:
                lines.append(f"    · {label}")
    return "\n".join(lines) if lines else "(해당 번들 없음)"


async def extract_handover(
    *,
    tier1: str,
    tier2: str,
    tier3: str,
    llm,
    lexicon: LexiconStore,
    patient_types: list[str] | None = None,
    bundle_ids: list[str] | None = None,
) -> dict:
    scales_data = lexicon.scales() or {}
    khna_data = lexicon.khna() or {}
    user = USER_TEMPLATE.format(
        tier1=tier1,
        tier2=tier2,
        tier3=tier3 or "(없음)",
        abbreviations="\n".join(f"- {e['abbr']}: {e['ko']}" for e in lexicon._abbr["entries"][:400]),
        symptom_groups="\n".join(f"- {g['id']} ({g['label']}): {', '.join(g['members'])}" for g in lexicon._symptoms.get("groups", [])),
        safety_categories="\n".join(f"- {c['id']} ({c['label']}): {c['enum']}" for c in lexicon.safety_categories()),
        clinical_scales=_format_clinical_scales(scales_data),
        documentation_focus=_format_documentation_focus(khna_data, patient_types or []),
        bundle_vocabulary=_format_bundle_vocabulary(khna_data, bundle_ids or []),
    )
    return await llm.chat_json(system=SYSTEM, user=user, json_schema=_LLM_SCHEMA, temperature=0.0)