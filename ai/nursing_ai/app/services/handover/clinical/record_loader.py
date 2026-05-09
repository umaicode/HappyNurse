from dataclasses import dataclass, field
from datetime import datetime
from typing import Protocol


@dataclass
class NfcMedRecord:
    admin_id: str
    encounter_id: str
    drug_name: str
    dose: str
    route: str
    administered_at: datetime


@dataclass
class NursingRecord:
    record_id: str
    encounter_id: str
    text: str
    ts: datetime
    line_count: int


@dataclass
class EncounterStatic:
    encounter_id: str
    admission_dx: str | None
    surgery: str | None
    pod: int | None
    hd: int | None
    allergies: str
    isolation: str
    dnr: str
    high_alert_meds: list[str]


@dataclass
class LoadedContext:
    tier1_text: str
    tier2: EncounterStatic
    tier3_text: str = ""
    tier1_records: list[NursingRecord] = field(default_factory=list)
    tier3_records: list[NursingRecord] = field(default_factory=list)
    last_record_ts: datetime | None = None
    tiers_used: list[str] = field(default_factory=lambda: ["tier1", "tier2"])


class RecordRepository(Protocol):
    async def fetch_records_in_window(self, encounter_id: str, start: datetime, end: datetime) -> list[NursingRecord]: ...
    async def fetch_static_facts(self, encounter_id: str) -> EncounterStatic: ...
    async def fetch_nfc_meds_in_window(self, encounter_id: str, start: datetime, end: datetime) -> list[NfcMedRecord]: ...


class RecordLoader:
    def __init__(self, repo: RecordRepository):
        self._repo = repo

    async def load(self, *, encounter_id: str, shift_start: datetime, shift_end: datetime) -> LoadedContext:
        tier1 = await self._repo.fetch_records_in_window(encounter_id, shift_start, shift_end)
        tier2 = await self._repo.fetch_static_facts(encounter_id)
        text = "\n".join(f"[{r.ts.isoformat()}|{r.record_id}] {r.text}" for r in tier1)
        last_ts = max((r.ts for r in tier1), default=None)
        return LoadedContext(tier1_text=text, tier1_records=list(tier1),
                             tier2=tier2, last_record_ts=last_ts)

    async def load_nfc_meds(self, *, encounter_id: str, shift_start: datetime, shift_end: datetime) -> list[NfcMedRecord]:
        return await self._repo.fetch_nfc_meds_in_window(encounter_id, shift_start, shift_end)

    async def extend_with_tier3(self, ctx: LoadedContext, *, encounter_id: str, prev_shift_start: datetime, prev_shift_end: datetime) -> LoadedContext:
        prev = await self._repo.fetch_records_in_window(encounter_id, prev_shift_start, prev_shift_end)
        ctx.tier3_records = list(prev)
        ctx.tier3_text = "\n".join(f"[{r.ts.isoformat()}|{r.record_id}] {r.text}" for r in prev)
        if "tier3" not in ctx.tiers_used:
            ctx.tiers_used.append("tier3")
        return ctx
