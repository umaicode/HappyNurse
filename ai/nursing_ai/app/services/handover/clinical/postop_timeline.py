"""POD + 증상 → 가능 합병증 후보 분류 (분류만, 진단 X).

5 W's of postoperative fever. 시스템은 후보만 표시, 진단 만들지 않음.
"""
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "postoperative_complication_timeline.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


def _pod_in_range(pod: int, range_str: str) -> bool:
    """range_str: 'POD 1-2', 'POD 3-5', 'POD 7+'"""
    s = range_str.replace("POD", "").strip()
    if s.endswith("+"):
        return pod >= int(s[:-1])
    if "-" in s:
        lo, hi = s.split("-")
        return int(lo) <= pod <= int(hi)
    return pod == int(s)


def suggest_complication_categories(*, pod: int | None, has_fever: bool, symptom_text: str) -> list[dict]:
    """5 W's 시점별 매칭. 시스템은 후보 분류만 표시, 진단 X.

    Returns:
        list of dicts with: pod_range, category, candidates, matched_findings, reference, source, note
    """
    if pod is None or not has_fever:
        return []
    data = _load()
    out = []
    symptom_lower = symptom_text.lower()
    for entry in data["postoperative_fever_5ws"]:
        if not _pod_in_range(pod, entry["pod_range"]):
            continue
        # 연관 소견 매칭 (소문자 비교, 한국어는 그대로 비교)
        matched_findings = []
        for finding in entry["associated_findings"]:
            if finding.lower() in symptom_lower:
                matched_findings.append(finding)
        out.append({
            "pod_range": entry["pod_range"],
            "category": entry["category"],
            "candidates": entry["common_causes_korean"],
            "matched_findings": matched_findings,
            "reference": entry.get("suggested_assessment_reference"),
            "source": data["source"],
            "note": "후보 분류일 뿐 진단 아님. 임상가 평가 필요.",
        })
    return out
