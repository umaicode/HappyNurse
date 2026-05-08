def scan_inference_terms(text: str, blocklist: dict) -> list[str]:
    text_low = text.lower()
    hits: list[str] = []
    for term in blocklist.get("ko", []):
        if term in text:
            hits.append(term)
    for term in blocklist.get("en", []):
        if term.lower() in text_low:
            hits.append(term)
    return hits


def lint_item(item: dict, blocklist: dict) -> str:
    """'ok' | 'blocked'. Layer 3 verbatim(quote 보유 또는 source_layer==3)은 항상 통과."""
    if item.get("source_layer") == 3 or "quote" in item:
        return "ok"
    text = item.get("value") or item.get("text") or ""
    return "blocked" if scan_inference_terms(text, blocklist) else "ok"
