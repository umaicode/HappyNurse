from pathlib import Path
from types import SimpleNamespace
import yaml


def _to_ns(v):
    if isinstance(v, dict):
        return SimpleNamespace(**{k: _to_ns(x) for k, x in v.items()})
    return v


class RuleEngine:
    def __init__(self, rules_dir: str | Path):
        d = Path(rules_dir)
        self._clinical = yaml.safe_load((d / "clinical_flags.yml").read_text(encoding="utf-8"))
        self._context = yaml.safe_load((d / "context_trigger.yml").read_text(encoding="utf-8"))

    def evaluate_clinical(self, ctx: dict) -> list[dict]:
        return self._evaluate(self._clinical.get("flags", []), ctx)

    def evaluate_context(self, ctx: dict) -> list[dict]:
        return self._evaluate(self._context.get("triggers", []), ctx)

    @staticmethod
    def _evaluate(rules: list[dict], ctx: dict) -> list[dict]:
        ns = {k: _to_ns(v) for k, v in ctx.items()}
        ns["any"] = any
        ns["all"] = all
        fired = []
        for r in rules:
            try:
                ok = bool(eval(r["when"], {"__builtins__": {}}, ns))  # noqa: S307
            except Exception:
                ok = False
            if ok:
                fired.append(r)
        return fired
