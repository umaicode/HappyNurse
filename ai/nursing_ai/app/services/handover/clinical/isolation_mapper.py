"""미생물 키워드 검출 → 격리 유형 라벨 (deterministic).

KHNA 의료기관 격리주의지침. 시스템이 격리를 명령하는 게 아니라
텍스트에서 사실 추출만.
"""
import re
from dataclasses import dataclass, field
from pathlib import Path
import yaml


_DATA_PATH = Path(__file__).parent.parent / "data" / "khna_extracted" / "isolation_microbe_mapping.yml"
_CACHE: dict | None = None


def _load() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = yaml.safe_load(_DATA_PATH.read_text(encoding="utf-8"))
    return _CACHE


@dataclass
class IsolationDetection:
    isolation_type: str           # contact / droplet / airborne / reverse_isolation
    microbe_name: str
    matched_text: str
    ppe_required: list[str] = field(default_factory=list)
    source: str = ""


def detect_isolation_requirements(text: str) -> list[IsolationDetection]:
    """텍스트에서 격리 적응증 자동 검출. 중복 미생물은 한 번만."""
    data = _load()
    out = []
    seen_microbes = set()

    for iso_type, iso_data in data["isolation_types"].items():
        microbes = iso_data.get("microbes", iso_data.get("indications", []))
        ppe = iso_data.get("ppe", [])
        for entry in microbes:
            pat = re.compile(entry["pattern"], re.IGNORECASE)
            m = pat.search(text)
            if m and entry["name"] not in seen_microbes:
                seen_microbes.add(entry["name"])
                out.append(IsolationDetection(
                    isolation_type=iso_type,
                    microbe_name=entry["name"],
                    matched_text=m.group(0),
                    ppe_required=ppe,
                    source="병원간호사회. 의료기관의 격리주의지침.",
                ))
    return out
