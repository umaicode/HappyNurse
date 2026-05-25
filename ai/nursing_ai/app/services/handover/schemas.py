from typing import Literal
from datetime import datetime
from pydantic import BaseModel, Field


class Citation(BaseModel):
    id: str
    record_id: str
    line_range: list[int]
    ts: datetime
    label: str
    excerpt: str | None = None


class SlotItem(BaseModel):
    kind: str | None = None
    value: str | None = None
    quote: str | None = None
    citation_ids: list[str] = Field(default_factory=list)
    confidence: float | None = None
    source_layer: Literal[1, 2, 3, 4]
    time_window: str | None = None
    trend: str | None = None
    contingency: str | None = None
    severity_flag: Literal["stable", "watcher", "unstable"] | None = None


class Slot(BaseModel):
    items: list[SlotItem] = Field(default_factory=list)
    verification: Literal["ok", "partial", "failed"] = "ok"


class Slots(BaseModel):
    patient_problem: Slot
    assessment: Slot
    situation: Slot
    safety: Slot
    background: Slot
    action: Slot
    recommendation: Slot
    synthesis: Slot = Field(default_factory=Slot)


class RuleFired(BaseModel):
    rule_id: str
    label: str
    source: str
    severity: Literal["low", "medium", "high"]
    matched_citation_ids: list[str] = Field(default_factory=list)


class Meta(BaseModel):
    model: str
    lexicon_version: str
    rule_set_version: str
    context_tiers_used: list[str]
    last_record_ts: datetime
    generated_at: datetime
    token_usage: dict[str, int] = Field(default_factory=dict)
    verifier: dict[str, list[str]] = Field(default_factory=dict)
    failures: list[dict] = Field(default_factory=list)


class ScoreEntry(BaseModel):
    """결정론적으로 계산된 검증 임상 스코어 1건.

    출력 형식: 점수 + 분류 라벨 + 출처. 행동 권고는 포함하지 않음 (추론 0.0 원칙).
    """
    scale_id: str               # morse / caprini / news2 / qsofa / braden / gcs / cam
    score: int | float
    classification: str         # high_risk_for_fall, positive_sepsis_possible 등
    sources: list[str] = Field(default_factory=list)
    incomplete: bool = False
    missing_inputs: list[str] = Field(default_factory=list)
    details: dict = Field(default_factory=dict)
    ui_color: str | None = None
    ui_severity: Literal["stable", "watcher", "unstable"] | None = None


class HandoverPayload(BaseModel):
    schema_version: Literal["2.1"] = "2.1"
    header: str
    illness_severity: Literal["stable", "watcher", "unstable"] = "stable"
    slots: Slots
    citations: list[Citation]
    rules_fired: list[RuleFired]
    scores: list[ScoreEntry] = Field(default_factory=list)
    meta: Meta


class RosterPatientItem(BaseModel):
    encounter_id: int
    handover_id: int
    header: str
    risk_score: int
    rules_fired_brief: list[str]
    verification_summary: dict[str, int]
    freshness: dict[str, int]


class RosterSummary(BaseModel):
    schema_version: Literal["1.0"] = "1.0"
    kind: Literal["roster_summary"] = "roster_summary"
    narrative_header: str
    stats: dict
    patients: list[RosterPatientItem]
    verification_followup: list[dict]
    meta: dict