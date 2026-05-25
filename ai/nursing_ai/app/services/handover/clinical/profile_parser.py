"""nursing_record 자유 텍스트 → 구조화 사실 추출.

LLM 사용 X, 정규식 기반 deterministic 추출. 추론 0.0 원칙 정합.
"""
import re
from dataclasses import dataclass, field


# ───────── 시간 블록 파싱 헬퍼 ─────────

_TIME_PREFIX = r"(?P<time>\d{1,2}:\d{2})"
_TIME_LINE_ONLY = re.compile(r"^\s*(\d{1,2}:\d{2})\s*$")
_TIME_LINE_INLINE = re.compile(r"^\s*(\d{1,2}:\d{2})\s+(.+)")


def _iter_time_blocks(text: str):
    """nursing_record을 시간 블록 단위로 yield.

    형식 1: 시간이 단독 라인 → 다음 시간까지의 본문이 블록
    형식 2: 시간 + 내용 한 라인

    yields: (time_str, content_text)
    """
    lines = text.splitlines()
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        m_only = _TIME_LINE_ONLY.match(line)
        m_inline = _TIME_LINE_INLINE.match(line)
        if m_only:
            time = m_only.group(1)
            content_parts = []
            j = i + 1
            while j < n and not (_TIME_LINE_ONLY.match(lines[j]) or _TIME_LINE_INLINE.match(lines[j])):
                content_parts.append(lines[j])
                j += 1
            yield time, "\n".join(content_parts)
            i = j
        elif m_inline:
            time = m_inline.group(1)
            content_parts = [m_inline.group(2)]
            j = i + 1
            while j < n and not (_TIME_LINE_ONLY.match(lines[j]) or _TIME_LINE_INLINE.match(lines[j])):
                content_parts.append(lines[j])
                j += 1
            yield time, "\n".join(content_parts)
            i = j
        else:
            i += 1


# ───────── 활력징후 ─────────

_BP_PAT = re.compile(r"BP\s*(\d{2,3})\s*/\s*(\d{2,3})", re.IGNORECASE)
_HR_PAT = re.compile(r"HR\s*(\d{2,3})", re.IGNORECASE)
_RR_PAT = re.compile(r"RR\s*(\d{1,3})", re.IGNORECASE)
_SPO2_PAT = re.compile(r"SpO2\s*(\d{2,3})", re.IGNORECASE)
_TEMP_PAT = re.compile(r"Temp\s*(\d{2,3}(?:\.\d+)?)", re.IGNORECASE)


@dataclass
class VitalsPoint:
    time: str | None
    bp_sys: int | None = None
    bp_dia: int | None = None
    hr: int | None = None
    rr: int | None = None
    spo2: int | None = None
    temp: float | None = None


def parse_vitals_series(text: str) -> list[VitalsPoint]:
    """시간별 활력징후 추출. 각 시간 블록 안에서 BP가 있으면 인식."""
    points = []
    for time, content in _iter_time_blocks(text):
        bp_m = _BP_PAT.search(content)
        if not bp_m:
            continue
        hr_m = _HR_PAT.search(content)
        rr_m = _RR_PAT.search(content)
        spo2_m = _SPO2_PAT.search(content)
        temp_m = _TEMP_PAT.search(content)
        points.append(VitalsPoint(
            time=time,
            bp_sys=int(bp_m.group(1)),
            bp_dia=int(bp_m.group(2)),
            hr=int(hr_m.group(1)) if hr_m else None,
            rr=int(rr_m.group(1)) if rr_m else None,
            spo2=int(spo2_m.group(1)) if spo2_m else None,
            temp=float(temp_m.group(1)) if temp_m else None,
        ))
    return points


# ───────── POD (수술 후 일수) ─────────

_POD = re.compile(r"POD\s*#?\s*(\d+)", re.IGNORECASE)


def parse_pod(text: str) -> int | None:
    """POD3, POD#5 같은 표기 인식."""
    m = _POD.search(text)
    return int(m.group(1)) if m else None


# ───────── 통증 NRS ─────────

_NRS_PAT = re.compile(r"NRS\s*(\d{1,2})\s*점?", re.IGNORECASE)
_NRS_TRIGGER = re.compile(r"NRS\s*\d{1,2}\s*점?\s*(이상|이하|초과|미만)", re.IGNORECASE)


@dataclass
class PainPoint:
    time: str | None
    score: int


def parse_pain_nrs(text: str) -> list[PainPoint]:
    """시간 블록 단위로 NRS 점수 추출.

    'NRS 7점 이상' 같은 조건형 트리거는 제외 (실 측정값만).
    """
    points = []
    # 조건형 트리거 위치 표시 (제외용)
    trigger_positions = {m.span() for m in _NRS_TRIGGER.finditer(text)}

    for time, content in _iter_time_blocks(text):
        for m in _NRS_PAT.finditer(content):
            # content 안의 위치를 text 안의 위치로 환산 어렵기에,
            # 같은 매치 substring에 '이상/이하/초과/미만'이 바로 뒤따르는지 확인
            tail = content[m.end():m.end() + 6]
            if any(kw in tail for kw in ["이상", "이하", "초과", "미만"]):
                continue
            points.append(PainPoint(time=time, score=int(m.group(1))))
    return points


# ───────── 과거력 (한국어·영문 정규화) ─────────

_HISTORY_PATTERNS = [
    ("DM", re.compile(r"\b(DM|당뇨|당뇨병|diabetes\s*mellitus|T2DM|T1DM)\b", re.IGNORECASE)),
    ("HTN", re.compile(r"\b(HTN|고혈압|hypertension)\b", re.IGNORECASE)),
    ("CHF", re.compile(r"\b(CHF|심부전|heart\s*failure)\b|EF\s*\d+%", re.IGNORECASE)),
    ("CKD", re.compile(r"\b(CKD|만성신질환|만성신부전|chronic\s*kidney|신부전)\b", re.IGNORECASE)),
    ("DVT", re.compile(r"\b(DVT|심부정맥혈전증|deep\s*vein\s*thrombosis|VTE)\b", re.IGNORECASE)),
    ("AF", re.compile(r"\b(AF|atrial\s*fibrillation|심방세동)\b", re.IGNORECASE)),
    ("CAD", re.compile(r"\b(CAD|관상동맥|coronary\s*artery|협심증|angina)\b", re.IGNORECASE)),
    ("COPD", re.compile(r"\b(COPD|만성폐쇄성폐질환|chronic\s*obstructive)\b", re.IGNORECASE)),
    ("ASTHMA", re.compile(r"\b(asthma|천식)\b", re.IGNORECASE)),
    ("CVA", re.compile(r"\b(CVA|stroke|뇌졸중|뇌경색|뇌출혈|ICH)\b", re.IGNORECASE)),
    ("CANCER", re.compile(r"\b(cancer|carcinoma|malignancy|악성종양|위암|폐암|간암|대장암|유방암)\b|\b암\b", re.IGNORECASE)),
    ("OSTEOPOROSIS", re.compile(r"\b(osteoporosis|골다공증)\b", re.IGNORECASE)),
    ("PARKINSON", re.compile(r"\b(parkinson|파킨슨)\b", re.IGNORECASE)),
    ("DEMENTIA", re.compile(r"\b(dementia|치매)\b", re.IGNORECASE)),
]


def parse_past_history(text: str) -> list[str]:
    """텍스트에서 과거력 키워드 정규화. ID 리스트 반환 (중복 제거)."""
    found = []
    for hist_id, pat in _HISTORY_PATTERNS:
        if pat.search(text):
            found.append(hist_id)
    return found


# ───────── BST (혈당) ─────────

_BST = re.compile(r"BST\s*(?:측정|결과)?\s*(\d{2,3})(?:\s*mg/dL)?", re.IGNORECASE)


def parse_bst(text: str) -> list[int]:
    """BST 측정값 추출."""
    return [int(m.group(1)) for m in _BST.finditer(text)]


# ───────── 자가 호소 ─────────

_HOSO_SENTENCE = re.compile(r"([^.\n]*호소[^.\n]*)", re.IGNORECASE)


@dataclass
class SelfReport:
    time: str | None
    text: str


def parse_self_reports(text: str) -> list[SelfReport]:
    """환자 자가 호소 추출 ('호소' 키워드 포함 문장)."""
    reports = []
    for time, content in _iter_time_blocks(text):
        for m in _HOSO_SENTENCE.finditer(content):
            reports.append(SelfReport(time=time, text=m.group(1).strip()))
    return reports


# ───────── 통합 ─────────

@dataclass
class PatientProfile:
    history_ids: list[str] = field(default_factory=list)
    pod: int | None = None
    vitals_series: list[VitalsPoint] = field(default_factory=list)
    pain_series: list[PainPoint] = field(default_factory=list)
    bst_series: list[int] = field(default_factory=list)
    self_reports: list[SelfReport] = field(default_factory=list)


def parse_profile(text: str) -> PatientProfile:
    """nursing_record 통합 파싱 → PatientProfile dataclass."""
    return PatientProfile(
        history_ids=parse_past_history(text),
        pod=parse_pod(text),
        vitals_series=parse_vitals_series(text),
        pain_series=parse_pain_nrs(text),
        bst_series=parse_bst(text),
        self_reports=parse_self_reports(text),
    )
