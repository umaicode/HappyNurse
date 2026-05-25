"""약물명 → 낙상위험점수·고위험약물 분류 (deterministic).

medication_fall_risk_classes.yml 기반. KHNA 낙상관리 도구 13 채택.
"""
import re
from dataclasses import dataclass, field
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "medication_fall_risk_classes.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


def _word_match(needle: str, haystack: str) -> bool:
    """단어 경계 기반 매치 — 'RI' 같은 짧은 약어가 'warfarin' 내부에 매치되는 false positive 방지.

    한국어는 단어 경계가 명확하지 않아 substring 매치 허용,
    영문은 단어 경계 강제.
    """
    needle_l = needle.lower()
    hay_l = haystack.lower()
    # 한국어 포함 시 substring 매치
    if any(ord(c) >= 0xAC00 and ord(c) <= 0xD7AF for c in needle):
        return needle_l in hay_l
    # 영문은 단어 경계 매치
    pattern = r"\b" + re.escape(needle_l) + r"\b"
    return bool(re.search(pattern, hay_l))


@dataclass
class MedicationClassification:
    drug_name: str
    fall_risk_score: int = 0
    classes: list[str] = field(default_factory=list)
    is_high_alert: bool = False
    high_alert_id: str | None = None
    sources: list[str] = field(default_factory=list)


@dataclass
class MedicationFallRiskResult:
    total_score: int
    classification: str
    contributing: list[MedicationClassification] = field(default_factory=list)
    sources: list[str] = field(default_factory=list)


def classify_medication(drug_text: str) -> MedicationClassification:
    """단일 약물 문자열 → 분류 (단어 경계 매치)."""
    data = _load()
    classes_found = []
    score = 0

    for tier_key, points in [("high_risk_3pts", 3), ("moderate_risk_2pts", 2), ("low_risk_1pt", 1)]:
        for cls in data["medication_classes"][tier_key]["classes"]:
            if any(_word_match(ex, drug_text) for ex in cls["examples"]):
                classes_found.append(cls["id"])
                score = max(score, points)

    # 고위험약물 분류 (단어 경계 매치, 잘못된 부분 매치 방지)
    is_ha = False
    ha_id = None
    for ha in data["high_alert_medications"]["list"]:
        examples = ha.get("examples", [ha["name_ko"]])
        if any(_word_match(ex, drug_text) for ex in examples) or _word_match(ha["id"], drug_text):
            is_ha = True
            ha_id = ha["id"]
            break

    return MedicationClassification(
        drug_name=drug_text,
        fall_risk_score=score,
        classes=classes_found,
        is_high_alert=is_ha,
        high_alert_id=ha_id,
        sources=[
            "병원간호사회. 낙상관리 임상간호실무지침. 도구 13.",
            "WHO/ISMP High-Alert Medications.",
        ],
    )


def compute_medication_fall_risk_score(drugs: list[str]) -> MedicationFallRiskResult:
    """여러 약물 통합 낙상위험 점수 (≥6점 = 고위험)."""
    data = _load()
    threshold = data["high_risk_threshold"]
    contributing = [classify_medication(d) for d in drugs]
    total = sum(c.fall_risk_score for c in contributing)
    cls = "high_risk_falls_from_meds" if total >= threshold else "moderate_or_low_meds_fall_risk"
    return MedicationFallRiskResult(
        total_score=total, classification=cls, contributing=contributing,
        sources=["병원간호사회. 낙상관리 임상간호실무지침. 도구 13."],
    )
