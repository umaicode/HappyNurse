"""검증된 임상 스코어 결정론적 계산.

출력: 점수 + 분류 라벨 + 출처. 행동 권고는 생성하지 않음 (추론 0.0 원칙).
모든 cutoff·점수표는 KHNA + 국제 1차 출처 인용.
"""
from dataclasses import dataclass, field
from typing import Literal


# ───────── NEWS2 (Royal College of Physicians, 2017) ─────────

NEWS2_SOURCES = [
    "Royal College of Physicians. National Early Warning Score (NEWS) 2. London: RCP, 2017.",
]


@dataclass
class NEWS2Result:
    score: int
    classification: str
    sources: list[str] = field(default_factory=list)
    incomplete: bool = False
    missing_inputs: list[str] = field(default_factory=list)
    component_scores: dict[str, int] = field(default_factory=dict)


def _news2_rr(rr: int | None) -> int:
    if rr is None: return 0
    if rr <= 8: return 3
    if rr <= 11: return 1
    if rr <= 20: return 0
    if rr <= 24: return 2
    return 3


def _news2_spo2(spo2: int | None) -> int:
    if spo2 is None: return 0
    if spo2 <= 91: return 3
    if spo2 <= 93: return 2
    if spo2 <= 95: return 1
    return 0


def _news2_temp(temp: float | None) -> int:
    if temp is None: return 0
    if temp <= 35.0: return 3
    if temp <= 36.0: return 1
    if temp <= 38.0: return 0
    if temp <= 39.0: return 1
    return 2


def _news2_bp(bp_sys: int | None) -> int:
    if bp_sys is None: return 0
    if bp_sys <= 90: return 3
    if bp_sys <= 100: return 2
    if bp_sys <= 110: return 1
    if bp_sys <= 219: return 0
    return 3


def _news2_hr(hr: int | None) -> int:
    if hr is None: return 0
    if hr <= 40: return 3
    if hr <= 50: return 1
    if hr <= 90: return 0
    if hr <= 110: return 1
    if hr <= 130: return 2
    return 3


def _classify_news2(score: int) -> str:
    if score >= 7: return "high_risk_clinical_deterioration"
    if score >= 5: return "moderate_risk"
    return "low_risk"


def compute_news2(*, rr: int | None, spo2: int | None, on_oxygen: bool,
                  temp: float | None, bp_sys: int | None, hr: int | None,
                  alert: bool = True) -> NEWS2Result:
    """RCP UK NEWS2 (2017) 점수 계산. 공식 표 그대로 코딩."""
    missing = [name for name, val in
               [("rr", rr), ("spo2", spo2), ("temp", temp),
                ("bp_sys", bp_sys), ("hr", hr)] if val is None]
    component = {
        "rr": _news2_rr(rr),
        "spo2": _news2_spo2(spo2),
        "o2": 2 if on_oxygen else 0,
        "temp": _news2_temp(temp),
        "bp_sys": _news2_bp(bp_sys),
        "hr": _news2_hr(hr),
        "consciousness": 0 if alert else 3,
    }
    score = sum(component.values())
    return NEWS2Result(
        score=score,
        classification=_classify_news2(score),
        sources=NEWS2_SOURCES,
        incomplete=bool(missing),
        missing_inputs=missing,
        component_scores=component,
    )


# ───────── qSOFA (Sepsis-3, JAMA 2016) ─────────

QSOFA_SOURCES = [
    "Singer M, et al. The Third International Consensus Definitions for Sepsis and Septic Shock (Sepsis-3). JAMA. 2016;315(8):801-810.",
]


@dataclass
class QSofaResult:
    score: int
    classification: str
    sources: list[str] = field(default_factory=list)


def compute_qsofa(*, rr: int | None, bp_sys: int | None,
                  altered_mental_status: bool) -> QSofaResult:
    score = 0
    if rr is not None and rr >= 22: score += 1
    if bp_sys is not None and bp_sys <= 100: score += 1
    if altered_mental_status: score += 1
    cls = "positive_sepsis_possible" if score >= 2 else "negative_for_sepsis"
    return QSofaResult(score=score, classification=cls, sources=QSOFA_SOURCES)


# ───────── Morse Fall Scale (KHNA + Morse 1989) ─────────

MORSE_SOURCES = [
    "병원간호사회. 낙상관리 임상간호실무지침. 도구 2.",
    "Morse JM, Black C, Oberle K. Development of a scale to identify the fall-prone patient. Can J Aging. 1989;8(4):366-77.",
]


@dataclass
class MorseResult:
    score: int
    classification: str
    item_scores: dict[str, int] = field(default_factory=dict)
    sources: list[str] = field(default_factory=list)


def compute_morse_fall(*,
    fall_history: bool,
    secondary_diagnosis: bool,
    ambulation_aid: Literal["none", "walker_or_cane", "furniture_holding"],
    iv_or_heparin_lock: bool,
    gait: Literal["normal", "weak", "impaired"],
    mental_status_oriented: bool,
) -> MorseResult:
    """KHNA 채택 Morse Fall Scale. cutoff: 0-24 저위험 / 25-44 중간 / ≥45 고위험."""
    item_scores = {
        "fall_history": 25 if fall_history else 0,
        "secondary_diagnosis": 15 if secondary_diagnosis else 0,
        "ambulation_aid": {"none": 0, "walker_or_cane": 15, "furniture_holding": 30}[ambulation_aid],
        "iv_or_heparin_lock": 20 if iv_or_heparin_lock else 0,
        "gait": {"normal": 0, "weak": 10, "impaired": 20}[gait],
        "mental_status": 0 if mental_status_oriented else 15,
    }
    score = sum(item_scores.values())
    if score >= 45: cls = "high_risk_for_fall"
    elif score >= 25: cls = "moderate_risk_for_fall"
    else: cls = "low_risk_for_fall"
    return MorseResult(
        score=score, classification=cls,
        item_scores=item_scores, sources=MORSE_SOURCES,
    )


# ───────── Caprini RAM (KHNA 2023 + Caprini 2010) ─────────

CAPRINI_SOURCES = [
    "병원간호사회. 정맥혈전색전증 예방간호 임상간호실무지침(2023 개정).",
    "Caprini JA. Risk assessment as a guide for the prevention of the many faces of venous thromboembolism. Am J Surg. 2010;199(1 Suppl):S3-10.",
    "Bahl V, et al. A validation study of a retrospective venous thromboembolism risk scoring method. Ann Surg. 2010;251(2):344-50.",
]


@dataclass
class CapriniResult:
    score: int
    classification: str
    factor_scores: dict[str, int] = field(default_factory=dict)
    sources: list[str] = field(default_factory=list)


def _caprini_age_score(age: int) -> int:
    if age >= 75: return 3
    if age >= 61: return 2
    if age >= 41: return 1
    return 0


def compute_caprini(*, age: int, major_surgery: bool, bed_rest_72h: bool,
                    cancer: bool, prior_vte: bool, bmi_gt_25: bool,
                    additional_factors: list[str] | None = None) -> CapriniResult:
    """Caprini RAM 단순화 — nursing_record에서 결정론적으로 잡히는 핵심 항목만.

    cutoff: 0-1 매우낮음 / 2 낮음 / 3-4 중등도 / ≥5 고위험.
    """
    if additional_factors is None:
        additional_factors = []
    factors = {
        "age": _caprini_age_score(age),
        "major_surgery": 2 if major_surgery else 0,
        "bed_rest_72h": 2 if bed_rest_72h else 0,
        "cancer": 2 if cancer else 0,
        "prior_vte": 3 if prior_vte else 0,
        "bmi_gt_25": 1 if bmi_gt_25 else 0,
    }
    for f in additional_factors:
        factors[f] = factors.get(f, 0) + 1
    score = sum(factors.values())
    if score >= 5: cls = "high_risk_VTE"
    elif score >= 3: cls = "moderate_risk_VTE"
    elif score == 2: cls = "low_risk_VTE"
    else: cls = "very_low_risk_VTE"
    return CapriniResult(
        score=score, classification=cls,
        factor_scores=factors, sources=CAPRINI_SOURCES,
    )


# ───────── Braden Scale (KHNA 2022 + Bergstrom 1987) ─────────

BRADEN_SOURCES = [
    "병원간호사회. 욕창간호 임상간호실무지침(2022 개정).",
    "Bergstrom N, Braden BJ, Laguzza A, Holman V. The Braden Scale for Predicting Pressure Sore Risk. Nurs Res. 1987;36(4):205-10.",
    "한국형 cutoff: 이영희 등 (2003).",
]


@dataclass
class BradenResult:
    score: int
    classification: str
    sources: list[str] = field(default_factory=list)


def compute_braden(*, sensory: int, moisture: int, activity: int,
                   mobility: int, nutrition: int, friction_shear: int) -> BradenResult:
    """Braden Scale 6항목 합산. 한국형 cutoff (≤12 고위험 / 13-14 중등도 / 15-18 경미 / ≥19 위험없음)."""
    score = sensory + moisture + activity + mobility + nutrition + friction_shear
    if score <= 12: cls = "high_risk_pressure_injury"
    elif score <= 14: cls = "moderate_risk_pressure_injury"
    elif score <= 18: cls = "mild_risk_pressure_injury"
    else: cls = "no_significant_risk"
    return BradenResult(score=score, classification=cls, sources=BRADEN_SOURCES)


# ───────── Glasgow Coma Scale (KHNA + Teasdale 1974) ─────────

GCS_SOURCES = [
    "병원간호사회. 낙상관리 임상간호실무지침. 권고 III-3.1.",
    "Teasdale G, Jennett B. Assessment of coma and impaired consciousness. Lancet. 1974;2(7872):81-4.",
]


@dataclass
class GCSResult:
    score: int
    classification: str
    components: dict[str, int] = field(default_factory=dict)
    sources: list[str] = field(default_factory=list)


def compute_gcs(*, eye: int, verbal: int, motor: int) -> GCSResult:
    """Glasgow Coma Scale 합산. 13-15 mild / 9-12 moderate / 3-8 severe(혼수)."""
    score = eye + verbal + motor
    if score >= 13: cls = "mild_injury_alert"
    elif score >= 9: cls = "moderate_injury"
    else: cls = "severe_injury_coma"
    return GCSResult(
        score=score, classification=cls,
        components={"eye": eye, "verbal": verbal, "motor": motor},
        sources=GCS_SOURCES,
    )
