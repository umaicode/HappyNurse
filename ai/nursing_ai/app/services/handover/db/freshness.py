from sqlalchemy.ext.asyncio import async_sessionmaker
from app.services.handover.db.models import ShiftHandover
from datetime import datetime


class FreshnessRepo:
    def __init__(self, *, session_factory: async_sessionmaker, record_repo):
        self._sf = session_factory
        self._records = record_repo

    async def count_new_records_since_report(self, handover_id: str) -> dict:
        async with self._sf() as s:
            row = await s.get(ShiftHandover, int(handover_id))
            if not row:
                return {"new_records_since_report": 0}
            ts = row.auto_summary_json.get("meta", {}).get("last_record_ts")
            if not ts:
                return {"new_records_since_report": 0}
            after_ts = datetime.fromisoformat(ts)
            cnt = await self._records.count_records_after(str(row.encounter_id), after_ts)
            return {"new_records_since_report": int(cnt)}