from pathlib import Path
import yaml


class LexiconStore:
    def __init__(self, lexicon_dir: str | Path):
        d = Path(lexicon_dir)
        self._abbr = self._load(d / "abbreviations.yml")
        self._symptoms = self._load(d / "symptoms.yml")
        self._safety = self._load(d / "safety_categories.yml")
        self._blocklist = self._load(d / "inference_blocklist.yml")
        # 검증 임상 척도 (기존 미사용 자산)
        scales_path = d / "assessment_scales.yml"
        self._scales = self._load(scales_path) if scales_path.exists() else {}
        # KHNA 추출 데이터 (8개 YAML)
        khna_dir = d.parent / "khna_extracted"
        self._khna: dict[str, dict] = {}
        if khna_dir.exists():
            for yml_path in khna_dir.glob("*.yml"):
                self._khna[yml_path.stem] = self._load(yml_path)
        # 인덱스
        self._abbr_index = {e["abbr"].lower(): e["ko"] for e in self._abbr["entries"]}
        self._symptom_index: dict[str, str] = {}
        for g in self._symptoms.get("groups", []):
            for m in g["members"]:
                self._symptom_index[m.lower()] = g["id"]

    @staticmethod
    def _load(p: Path):
        return yaml.safe_load(p.read_text(encoding="utf-8"))

    def expand_abbr(self, token: str) -> str | None:
        return self._abbr_index.get(token.lower())

    def match_symptom_group(self, token: str) -> str | None:
        return self._symptom_index.get(token.lower())

    def safety_categories(self):
        return self._safety["categories"]

    def inference_blocklist(self):
        return self._blocklist

    def scales(self) -> dict:
        """assessment_scales.yml — NRS/MFS/Braden/GCS/qSOFA/NEWS2/RASS/Caprini/KTAS 등.

        action/monitoring 필드(추론 권고)는 제거 후 반환 — 추론 0.0 원칙 정합.
        """
        if not self._scales:
            return {}
        cleaned_scales = []
        for scale in self._scales.get("scales", []):
            cleaned = {k: v for k, v in scale.items() if k not in ("action", "monitoring", "note")}
            if "thresholds" in cleaned and isinstance(cleaned["thresholds"], list):
                cleaned["thresholds"] = [
                    {kk: vv for kk, vv in (t.items() if isinstance(t, dict) else [])
                     if kk not in ("action", "monitoring")}
                    for t in cleaned["thresholds"]
                ]
            cleaned_scales.append(cleaned)
        return {"scales": cleaned_scales, "version": self._scales.get("version")}

    def khna(self) -> dict:
        """KHNA 추출 데이터 8개 YAML 통합 dict.

        키: 파일 stem (예: 'documentation_standards', 'bundle_care_checklists', ...).
        """
        return self._khna
