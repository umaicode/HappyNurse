"""환자 컨디션 자동 분류.

documentation_standards.yml + bundle_care_checklists.yml의 condition/bundle 키와
일치하는 카테고리 ID를 반환. LLM prompt에 환자별 표준 사정 항목·번들 어휘를
컨디션 주입할 때 사용.

추론 X — 단순 키워드/조건 매칭.
"""
import re
from app.services.handover.clinical.record_loader import EncounterStatic


_DEVICE_PATTERNS = {
    "artificial_airway": re.compile(
        r"ventilator|기관내삽관|기관삽관|\bETT\b|T-piece|인공호흡기|기관절개", re.IGNORECASE
    ),
    "iv_central": re.compile(
        r"\bCVC\b|중심정맥관|central\s*line|\bPICC\b|중심정맥카테터", re.IGNORECASE
    ),
    "indwelling_catheter": re.compile(
        r"Foley|유치도뇨|도뇨관|indwelling\s*cath", re.IGNORECASE
    ),
    "enteral_nutrition": re.compile(
        r"L-tube|비위관|\bNGT\b|\bPEG\b|G-tube|경관영양|경장영양", re.IGNORECASE
    ),
    "iv_peripheral": re.compile(
        r"\bIV\b|정맥주사|말초정맥|peripheral\s*line|\b1[68]G\b|\b2[02]G\b", re.IGNORECASE
    ),
    "ostomy": re.compile(
        r"ostomy|장루|colostomy|ileostomy|결장루|회장루", re.IGNORECASE
    ),
}

_FALL_RISK_KEYWORDS = re.compile(
    r"낙상|보행기|walker|어지러움|기립성저혈압|기립성\s*저혈압", re.IGNORECASE
)

_PAIN_KEYWORDS = re.compile(
    r"NRS\s*\d+|통증\b|pain\b|진통제|analgesic|뻐근|쑤심|저림", re.IGNORECASE
)


# documentation_standards.yml key ↔ bundle_care_checklists.yml key 매핑
_BUNDLE_MAP = {
    "artificial_airway": "vap_bundle",
    "iv_central": "clabsi_bundle",
    "indwelling_catheter": "cauti_bundle",
    "enteral_nutrition": "enteral_nutrition_safety",
}


def categorize_patient(
    *,
    tier2: EncounterStatic,
    nursing_record_text: str,
) -> list[str]:
    """환자 condition 분류.

    Returns:
        documentation_standards.yml의 키와 일치하는 condition ID 리스트.
        예: ["fall_high_risk", "vte_high_risk", "pain_active", "iv_peripheral"]
    """
    types: list[str] = []
    text = nursing_record_text or ""

    if tier2.surgery or _FALL_RISK_KEYWORDS.search(text):
        types.append("fall_high_risk")
    if tier2.surgery:
        types.append("vte_high_risk")
    if _PAIN_KEYWORDS.search(text):
        types.append("pain_active")

    for category, pattern in _DEVICE_PATTERNS.items():
        if pattern.search(text):
            types.append(category)

    return types


def applicable_bundles(patient_types: list[str]) -> list[str]:
    """환자 condition → 적용 번들 ID 매핑."""
    return [_BUNDLE_MAP[t] for t in patient_types if t in _BUNDLE_MAP]
