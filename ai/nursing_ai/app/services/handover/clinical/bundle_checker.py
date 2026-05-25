"""Bundle Care 항목 누락 검출 (VAP/CLABSI/CAUTI).

시스템 권고 X — 사실 부재 진술 (데이터 갭 ALERT).
"""
from dataclasses import dataclass, field
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "bundle_care_checklists.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


@dataclass
class BundleCheckResult:
    bundle_id: str
    applicable: bool
    present_items: list[dict] = field(default_factory=list)
    missing_items: list[dict] = field(default_factory=list)
    sources: list[str] = field(default_factory=list)


# 번들 항목별 텍스트 매칭 키워드 (KHNA 표준 용어 + 일반 표현)
_ITEM_KEYWORDS = {
    "hob_elevation": ["HOB", "침상머리", "좌위", "반좌위", "30도", "45도"],
    "hob_elevation_30": ["HOB", "침상머리", "30도", "45도", "좌위"],
    "oral_care_chg": ["구강간호", "구강 간호", "CHG", "클로르헥시딘", "oral care"],
    "oral_care": ["구강간호", "구강 간호", "oral care"],
    "sedation_break": ["진정 평가", "진정 중단", "RASS", "sedation break", "sedation vacation"],
    "dvt_prophylaxis": ["DVT 예방", "IPC", "탄력스타킹", "압박스타킹", "SCD", "GCS 착용", "AES"],
    "pud_prophylaxis": ["PUD 예방", "PPI", "H2 차단제", "famotidine", "omeprazole"],
    "suction_pressure": ["흡인 압력", "흡인 시간", "suction"],
    "hand_hygiene": ["손위생", "hand hygiene", "손 위생"],
    "sterile_technique": ["무균술", "무균 조작", "sterile"],
    "chg_dressing": ["CHG 드레싱", "클로르헥시딘 드레싱"],
    "daily_assessment": ["필요성 평가", "제거 평가"],
    "site_inspection": ["삽입부위 사정", "부위 확인", "삽입부위 확인"],
    "indication_check": ["적응증 확인", "삽입 적응증"],
    "sterile_insertion": ["무균 삽입"],
    "closed_drainage": ["폐쇄 배뇨", "closed drainage"],
    "daily_removal_review": ["제거 평가", "제거 고려"],
    "meatal_care": ["요도구 청결", "요도구 간호"],
    "grv_check": ["GRV", "위잔여량", "잔여량 확인"],
    "tube_position_verify": ["튜브 위치", "tube position"],
}


def _item_present(item_id: str, text: str) -> bool:
    keywords = _ITEM_KEYWORDS.get(item_id, [])
    return any(kw.lower() in text.lower() for kw in keywords)


def check_bundle_compliance(*, bundle_id: str, device_present: bool, shift_notes_text: str) -> BundleCheckResult:
    """번들 항목 누락 검출.

    Args:
        bundle_id: vap_bundle / clabsi_bundle / cauti_bundle / enteral_nutrition_safety
        device_present: 해당 디바이스(인공기도/CVC/Foley/L-tube) 유지 여부
        shift_notes_text: 시프트 nursing_record 본문
    """
    data = _load()
    bundle = data["bundles"].get(bundle_id)
    if not bundle:
        raise ValueError(f"Unknown bundle: {bundle_id}")
    if not device_present:
        return BundleCheckResult(bundle_id=bundle_id, applicable=False)
    present, missing = [], []
    for item in bundle["items"]:
        if _item_present(item["id"], shift_notes_text):
            present.append(item)
        else:
            missing.append(item)
    sources = bundle["source"] if isinstance(bundle["source"], list) else [bundle["source"]]
    return BundleCheckResult(
        bundle_id=bundle_id, applicable=True,
        present_items=present, missing_items=missing,
        sources=sources,
    )
