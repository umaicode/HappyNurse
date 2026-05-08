from pathlib import Path
import yaml


class LexiconStore:
    def __init__(self, lexicon_dir: str | Path):
        d = Path(lexicon_dir)
        self._abbr = self._load(d / "abbreviations.yml")
        self._symptoms = self._load(d / "symptoms.yml")
        self._safety = self._load(d / "safety_categories.yml")
        self._blocklist = self._load(d / "inference_blocklist.yml")
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
