"""KHNA 손상고위험 ABCs 평가 (Age/Bone/Coagulation/Surgery).

KHNA 낙상관리 임상간호실무지침 권고 III-1.15.
시스템은 위험 요인 감지·집계만 수행. 권고 생성 X (추론 0.0).
"""
import re
from dataclasses import dataclass, field
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "injury_high_risk_abcs.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


def _word_match_any(terms: list[str], haystack: str) -> list[str]:
    """terms 중 haystack에 등장하는 것들 반환. 한국어 substring, 영문 단어 경계."""
    matched: list[str] = []
    hay_l = haystack.lower()
    for t in terms:
        t_l = t.lower()
        if any(0xAC00 <= ord(c) <= 0xD7AF for c in t):
            if t_l in hay_l:
                matched.append(t)
        else:
            if re.search(r"\b" + re.escape(t_l) + r"\b", hay_l):
                matched.append(t)
    return matched


@dataclass
class FactorMatch:
    category: str
    label: str
    conditions_matched: list[str] = field(default_factory=list)


@dataclass
class InjuryRiskEvaluation:
    factors: list[FactorMatch] = field(default_factory=list)
    is_high_risk: bool = False
    severity_flag: str | None = None
    sources: list[str] = field(default_factory=list)


_SURGERY_KEYWORDS = [
    "흉부", "복부", "thoracic", "abdominal",
    "절단", "amputation",
    "골반", "pelvic", "무릎", "knee", "TKA", "THA",
    "spine", "척추", "fusion", "PLIF", "ACDF",
]


def evaluate_injury_risk(
    *,
    age: int | None,
    nursing_record_text: str,
    surgery_name: str | None,
    admission_dx: str | None,
    history_ids: list[str],
    medications: list[str],
) -> InjuryRiskEvaluation:
    """ABCs 4 카테고리 평가. 발견된 요인 ≥2면 high_risk, ==1이면 watcher."""
    data = _load()
    factors_data = data["abcs_factors"]
    factors_found: list[FactorMatch] = []

    full_text = " ".join([
        nursing_record_text or "",
        surgery_name or "",
        admission_dx or "",
        " ".join(medications or []),
    ])

    # A - Age
    age_threshold = factors_data["A_age"]["threshold_years"]
    if age is not None and age >= age_threshold:
        factors_found.append(FactorMatch(
            category="A_age",
            label=factors_data["A_age"]["label"],
            conditions_matched=[f"{age}세 (≥{age_threshold})"],
        ))

    # B - Bone disorder
    bone_matched: list[str] = []
    for cond in factors_data["B_bone_disorder"]["conditions"]:
        hits = _word_match_any(cond["terms"], full_text)
        if hits:
            bone_matched.append(cond["name"])
        elif cond["id"] == "osteoporosis" and "OSTEOPOROSIS" in history_ids:
            bone_matched.append(cond["name"])
    if bone_matched:
        factors_found.append(FactorMatch(
            category="B_bone_disorder",
            label=factors_data["B_bone_disorder"]["label"],
            conditions_matched=bone_matched,
        ))

    # C - Coagulation disorder
    coag_matched: list[str] = []
    for cond in factors_data["C_coagulation_disorder"]["conditions"]:
        hits = _word_match_any(cond["terms"], full_text)
        if hits:
            coag_matched.append(cond["name"])
    if coag_matched:
        factors_found.append(FactorMatch(
            category="C_coagulation_disorder",
            label=factors_data["C_coagulation_disorder"]["label"],
            conditions_matched=coag_matched,
        ))

    # S - Surgery
    surgery_matched: list[str] = []
    if surgery_name:
        surgery_text = surgery_name + " " + (admission_dx or "")
        for hs in factors_data["S_surgery"]["specific_high_risk_surgeries"]:
            if hs["name"] in surgery_text:
                surgery_matched.append(hs["name"])
        if not surgery_matched:
            for kw in _SURGERY_KEYWORDS:
                if kw.lower() in surgery_text.lower():
                    surgery_matched.append(f"고위험 수술({kw})")
                    break
    if surgery_matched:
        factors_found.append(FactorMatch(
            category="S_surgery",
            label=factors_data["S_surgery"]["label"],
            conditions_matched=surgery_matched,
        ))

    sev: str | None = None
    high_risk = False
    if len(factors_found) >= 2:
        sev = "unstable"
        high_risk = True
    elif len(factors_found) == 1:
        sev = "watcher"

    return InjuryRiskEvaluation(
        factors=factors_found,
        is_high_risk=high_risk,
        severity_flag=sev,
        sources=[data["source"]],
    )
