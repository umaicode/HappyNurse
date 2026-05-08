from dataclasses import dataclass
from app.services.handover.rules.engine import RuleEngine


@dataclass
class TriggerDecision:
    fetch: bool
    matched: list[dict]


def should_fetch_tier3(engine: RuleEngine, *, ctx: dict) -> TriggerDecision:
    fired = engine.evaluate_context(ctx)
    return TriggerDecision(fetch=bool(fired), matched=fired)