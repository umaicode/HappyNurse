import re
from datetime import datetime, date
from sqlalchemy.ext.asyncio import async_sessionmaker
from sqlalchemy import text

from app.services.handover.clinical.record_loader import (
    NursingRecord, NfcMedRecord, EncounterStatic
)


class DBRecordRepository:
    def __init__(self, session_factory: async_sessionmaker):
        self._sf = session_factory

    async def fetch_records_in_window(
        self, encounter_id: str, start: datetime, end: datetime
    ) -> list[NursingRecord]:
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT nursing_record_id, encounter_id, final_content, confirmed_at
                FROM nursing_record
                WHERE encounter_id = :eid
                AND status = 'confirmed'
                AND confirmed_at BETWEEN :start AND :end
                ORDER BY confirmed_at ASC
            """), {"eid": int(encounter_id), "start": start.replace(tzinfo=None), "end": end.replace(tzinfo=None)})
            rows = result.mappings().all()
        return [
            NursingRecord(
                record_id=str(r["nursing_record_id"]),
                encounter_id=str(r["encounter_id"]),
                text=r["final_content"] or "",
                ts=r["confirmed_at"],
                line_count=len((r["final_content"] or "").splitlines()),
            )
            for r in rows
        ]

    async def fetch_static_facts(self, encounter_id: str) -> EncounterStatic:
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT encounter_id, disease_name, surgery_name,
                        period_start, birth_date, bed_name
                FROM encounter
                WHERE encounter_id = :eid
            """), {"eid": int(encounter_id)})
            row = result.mappings().first()

        if not row:
            return EncounterStatic(
                encounter_id=encounter_id,
                admission_dx=None, surgery=None,
                pod=None, hd=None,
                allergies="미확인", isolation="미확인", dnr="미확인",
                high_alert_meds=[],
            )

        hd = _calc_hd(row.get("period_start"))

        return EncounterStatic(
            encounter_id=encounter_id,
            admission_dx=row.get("disease_name"),
            surgery=row.get("surgery_name"),
            pod=None,
            hd=hd,
            allergies="미확인",
            isolation="미확인",
            dnr="미확인",
            high_alert_meds=[],
        )

    async def fetch_nfc_meds_in_window(
        self, encounter_id: str, start: datetime, end: datetime
    ) -> list[NfcMedRecord]:
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT
                    ma.medication_admin_id,
                    ma.encounter_id,
                    m.product_name   AS drug_name,
                    ma.dosage_quantity,
                    ma.dosage_unit,
                    mo.route,
                    ma.effective_datetime
                FROM medication_administration ma
                JOIN medication m
                    ON ma.medication_id = m.medication_id
                LEFT JOIN medication_order mo
                    ON ma.medication_order_id = mo.medication_order_id
                WHERE ma.encounter_id = :eid
                AND ma.status = 'confirmed'
                AND ma.effective_datetime BETWEEN :start AND :end
                ORDER BY ma.effective_datetime ASC
            """), {"eid": int(encounter_id), "start": start.replace(tzinfo=None), "end": end.replace(tzinfo=None)})
            rows = result.mappings().all()
        return [
            NfcMedRecord(
                admin_id=str(r["medication_admin_id"]),
                encounter_id=str(r["encounter_id"]),
                drug_name=r["drug_name"] or "약물명 미확인",
                dose=str(r["dosage_quantity"] or ""),
                route=r["route"] or "",
                administered_at=r["effective_datetime"],
            )
            for r in rows
        ]

    async def count_records_after(self, encounter_id: str, after_ts: datetime) -> int:
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT COUNT(*)
                FROM nursing_record
                WHERE encounter_id = :eid
                AND status = 'confirmed'
                AND confirmed_at > :after
            """), {"eid": int(encounter_id), "after": after_ts.replace(tzinfo=None)})
            return result.scalar_one()


class DBRosterRepository:
    def __init__(self, session_factory: async_sessionmaker):
        self._sf = session_factory

    async def fetch_active_encounters(self, practitioner_id: str) -> list[str]:
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT encounter_id
                FROM encounter
                WHERE assigned_practitioner_id = :pid
                AND status = 'in_progress'
            """), {"pid": int(practitioner_id)})
            rows = result.all()
        return [str(r[0]) for r in rows]


def _calc_hd(period_start) -> int | None:
    if not period_start:
        return None
    try:
        if isinstance(period_start, datetime):
            return (date.today() - period_start.date()).days + 1
        if isinstance(period_start, date):
            return (date.today() - period_start).days + 1
    except Exception:
        pass
    return None