"""점수 → KHNA 분류 라벨 매핑 (deterministic, 추론 없음).

risk_classification_tables.yml 기반.
"""
from dataclasses import dataclass
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "risk_classification_tables.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


@dataclass
class ClassificationResult:
    category: str
    score: int | float
    label: str
    ui_color: str | None = None
    ui_severity: str | None = None
    intervention_recommended: bool | None = None
    action: str | None = None
    source: str | None = None


def _score_in_range(score: int | float, range_str: str) -> bool:
    """range_str 형식: '0-24', '≥45', '≤12'"""
    s = range_str.strip()
    if s.startswith("≥"):
        return score >= int(s[1:])
    if s.startswith("≤"):
        return score <= int(s[1:])
    if s.startswith("<"):
        return score < int(s[1:])
    if s.startswith(">"):
        return score > int(s[1:])
    if "-" in s:
        lo, hi = s.split("-")
        return int(lo) <= score <= int(hi)
    return score == int(s)


def classify_score(category: str, score: int | float) -> ClassificationResult:
    """점수를 KHNA 분류 라벨로 매핑.

    Args:
        category: fall_risk / vte_risk / pain_intensity / gcs / news2 / qsofa / braden
        score: 계산된 점수

    Returns:
        ClassificationResult — 라벨·색상·severity·source 포함
    """
    data = _load()
    classifications = data["classifications"]
    if category not in classifications:
        raise ValueError(f"Unknown classification category: {category}")
    table = classifications[category]
    source = table.get("source", "")
    for row in table["score_to_label"]:
        if _score_in_range(score, row["score_range"]):
            return ClassificationResult(
                category=category, score=score,
                label=row["label"],
                ui_color=row.get("ui_color"),
                ui_severity=row.get("ui_severity"),
                intervention_recommended=row.get("intervention_recommended"),
                action=row.get("action"),
                source=source,
            )
    raise ValueError(f"Score {score} did not match any range in category {category}")
