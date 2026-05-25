SLOT_TITLES = [
    ("patient_problem",  "[P] Patient/Problem"),
    ("assessment",       "[A] Assessment"),
    ("situation",        "[S] Situation"),
    ("safety",           "[S] Safety"),
    ("background",       "[B] Background"),
    ("action",           "[A] Action List"),
    ("recommendation",   "[R] Recommendation"),
    ("synthesis",        "[↩] 인수자 확인 체크리스트"),
]

_SEVERITY_BADGE = {
    "stable":   "🟢 STABLE",
    "watcher":  "🟡 WATCHER",
    "unstable": "🔴 UNSTABLE",
}


def _severity_prefix(item: dict) -> str:
    sev = item.get("severity_flag")
    if sev == "unstable":
        return "🔴 "
    if sev == "watcher":
        return "🟡 "
    return ""


def _item_line(item: dict, slot_key: str) -> str:
    text = item.get("value") or item.get("quote") or ""
    cites = ",".join(item.get("citation_ids", []))
    cite_str = f" [출처:{cites}]" if cites else ""
    sev = _severity_prefix(item)

    if slot_key == "synthesis":
        return f"□ {sev}{text}"

    if slot_key == "action":
        tw = item.get("time_window")
        if tw and tw != "PRN":
            return f"⏰ {tw}  {sev}{text}{cite_str}"
        if tw == "PRN":
            return f"⚠️ PRN  {sev}{text}{cite_str}"
        return f"- {sev}{text}{cite_str}"

    if slot_key == "recommendation":
        contingency = item.get("contingency")
        if contingency:
            # 버그 수정: value(text)도 함께 출력
            return f"- {sev}{text}\n  └ {contingency}{cite_str}"

    return f"- {sev}{text}{cite_str}"


def render_markdown(payload: dict) -> str:
    severity = payload.get("illness_severity", "stable")
    badge = _SEVERITY_BADGE.get(severity, "🟢 STABLE")

    lines = [
        f"**{payload['header']}**",
        "",
        f"## [I] 중증도: {badge}",
        "",
    ]

    # 임상 스코어는 pipeline에서 I-PASS-BAR 슬롯(assessment/safety)에 통합되므로
    # 별도 섹션을 렌더하지 않음. payload["scores"]는 프론트 호환용으로 유지.

    partial_slots: list[str] = []
    for key, title in SLOT_TITLES:
        slot = payload["slots"].get(key, {"items": [], "verification": "ok"})
        lines.append(f"## {title}")
        if not slot["items"]:
            lines.append("- 미기재")
        else:
            for it in slot["items"]:
                lines.append(_item_line(it, key))
        if slot["verification"] != "ok":
            partial_slots.append(title)
        lines.append("")

    rf = payload.get("rules_fired", [])
    if rf:
        lines.append("## 임상 규칙 플래그")
        for r in rf:
            lines.append(f"- [{r['label']}] (source: {r['source']}, severity: {r['severity']})")
        lines.append("")

    if partial_slots:
        lines.append("## 검증 결과")
        for s in partial_slots:
            lines.append(f"- {s}: 검증 미통과 항목 존재 — 면대면에서 원문 확인 필요")
        lines.append("")

    m = payload["meta"]
    lines.append("---")
    lines.append(f"*생성: {m['generated_at']} · 모델: {m['model']} · 어휘집: {m['lexicon_version']} · 룰셋: {m['rule_set_version']}*")
    lines.append(f"*포함된 마지막 간호기록 시각: {m['last_record_ts']}*")
    lines.append(f"*컨텍스트 사용 범위: {', '.join(m['context_tiers_used'])}*")
    return "\n".join(lines)
