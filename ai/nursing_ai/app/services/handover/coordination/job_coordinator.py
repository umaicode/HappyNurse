import asyncio, uuid
from collections import defaultdict


class JobCoordinator:
    def __init__(self):
        self._queues: dict[str, asyncio.Queue] = defaultdict(asyncio.Queue)
        self._patients: dict[str, list[str]] = {}

    def create_job(self, encounter_ids: list[str]) -> str:
        job_id = str(uuid.uuid4())
        self._patients[job_id] = list(encounter_ids)
        return job_id

    async def _put(self, job_id: str, evt: dict):
        await self._queues[job_id].put(evt)

    async def emit_progress(self, job_id: str, encounter_id: str, status: str):
        await self._put(job_id, {"event": "progress", "data": {"encounter_id": encounter_id, "status": status}})

    async def emit_complete(self, job_id: str, encounter_id: str, *, handover_id: str, verification_summary: dict):
        await self._put(job_id, {"event": "complete", "data": {
            "encounter_id": encounter_id, "handover_id": handover_id,
            "verification_summary": verification_summary,
        }})

    async def emit_error(self, job_id: str, encounter_id: str, *, reason: str, fallback_handover_id: str | None):
        await self._put(job_id, {"event": "error", "data": {
            "encounter_id": encounter_id, "reason": reason,
            "fallback_handover_id": fallback_handover_id,
        }})

    async def emit_roster_summary(self, job_id: str, *, payload: dict):
        await self._put(job_id, {"event": "roster_summary", "data": payload})

    async def emit_job_done(self, job_id: str):
        total = len(self._patients[job_id])
        await self._put(job_id, {"event": "job_done", "data": {"job_id": job_id, "total": total, "complete": total}})
        await self._queues[job_id].put(None)

    async def stream(self, job_id: str):
        q = self._queues[job_id]
        while True:
            evt = await q.get()
            if evt is None:
                return
            yield evt
            if evt["event"] == "job_done":
                _ = await q.get()
                return
