from app.services.handover.processing.lint import lint_item


def enforce_citation(slots: dict) -> dict:
    out = {}
    for name, slot in slots.items():
        kept = [i for i in slot.get("items", []) if i.get("citation_ids")]
        dropped = len(slot.get("items", [])) - len(kept)
        verification = "partial" if dropped > 0 else slot.get("verification", "ok")
        out[name] = {"items": kept, "verification": verification}
    return out


def apply_lint(slots: dict, blocklist: dict) -> dict:
    out = {}
    for name, slot in slots.items():
        kept, dropped = [], 0
        for item in slot.get("items", []):
            if lint_item(item, blocklist) == "ok":
                kept.append(item)
            else:
                dropped += 1
        verification = "partial" if dropped > 0 else slot.get("verification", "ok")
        out[name] = {"items": kept, "verification": verification}
    return out