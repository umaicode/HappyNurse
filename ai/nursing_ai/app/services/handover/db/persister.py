from sqlalchemy.ext.asyncio import AsyncSession
from app.services.handover.db.models import ShiftHandover


async def save_handover(
    session: AsyncSession, *,
    encounter_id,
    from_practitioner_id,
    text: str,
    payload: dict,
) -> ShiftHandover:
    h = ShiftHandover(
        encounter_id=encounter_id,
        from_practitioner_id=from_practitioner_id,
        auto_summary=text,
        auto_summary_json=payload,
    )
    session.add(h)
    await session.flush()
    return h


class Persister:
    async def save(self, *, session, encounter_id, from_practitioner_id, text, payload):
        return await save_handover(
            session=session,
            encounter_id=encounter_id,
            from_practitioner_id=from_practitioner_id,
            text=text,
            payload=payload,
        )