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


async def extract_handover(*, tier1: str, tier2: str, tier3: str, llm, lexicon: LexiconStore) -> dict:
    user = USER_TEMPLATE.format(
        tier1=tier1,
        tier2=tier2,
        tier3=tier3 or "(없음)",
        abbreviations="\n".join(f"- {e['abbr']}: {e['ko']}" for e in lexicon._abbr["entries"][:400]),
        symptom_groups="\n".join(f"- {g['id']} ({g['label']}): {', '.join(g['members'])}" for g in lexicon._symptoms.get("groups", [])),
        safety_categories="\n".join(f"- {c['id']} ({c['label']}): {c['enum']}" for c in lexicon.safety_categories()),
    )
    return await llm.chat_json(system=SYSTEM, user=user, json_schema=_LLM_SCHEMA, temperature=0.0)