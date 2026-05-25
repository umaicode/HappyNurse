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
                SELECT e.encounter_id, e.disease_name, e.surgery_name,
                        e.period_start, e.birth_date, e.bed_name,
                        e.department_code, e.chief_complaint,
                        e.name AS patient_name,
                        e.attending_physician_id,
                        p.identifier_value AS mrn,
                        r.name AS room_name
                FROM encounter e
                LEFT JOIN patient p ON e.patient_id = p.patient_id
                LEFT JOIN room r ON e.room_id = r.room_id
                WHERE e.encounter_id = :eid
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
        age = _calc_age(row.get("birth_date"))
        room_label = None
        if row.get("room_name"):
            bed = row.get("bed_name", "")
            room_label = f"{row['room_name']}-{bed}" if bed else row["room_name"]
        elif row.get("bed_name"):
            room_label = row["bed_name"]

        return EncounterStatic(
            encounter_id=encounter_id,
            admission_dx=row.get("disease_name"),
            surgery=row.get("surgery_name"),
            pod=None,  # nursing_record 텍스트에서 추출
            hd=hd,
            allergies="미확인",  # LLM이 nursing_record에서 추출
            isolation="미확인",
            dnr="미확인",
            high_alert_meds=[],
            age=age,
            department_code=row.get("department_code"),
            chief_complaint=row.get("chief_complaint"),
            patient_name=row.get("patient_name"),
            mrn=row.get("mrn"),
            room_label=room_label,
            attending_practitioner_id=str(row["attending_physician_id"]) if row.get("attending_physician_id") else None,
        )

    async def fetch_latest_handover(self, encounter_id: str) -> dict | None:
        """직전 ShiftHandover의 background + safety + scores 회수.
        인계 체인을 통한 환자 프로필 영속화.
        """
        async with self._sf() as session:
            result = await session.execute(text("""
                SELECT auto_summary_json
                FROM shift_handover
                WHERE encounter_id = :eid
                ORDER BY created_at DESC
                LIMIT 1
            """), {"eid": int(encounter_id)})
            row = result.scalar_one_or_none()
        if not row:
            return None
        payload = row if isinstance(row, dict) else {}
        slots = payload.get("slots", {}) if isinstance(payload, dict) else {}
        return {
            "background_items": (slots.get("background", {}) or {}).get("items", []),
            "safety_items": (slots.get("safety", {}) or {}).get("items", []),
            "scores": payload.get("scores", []) if isinstance(payload, dict) else [],
        }

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


def _calc_age(birth_date) -> int | None:
    """birth_date → 만 나이 계산."""
    if not birth_date:
        return None
    try:
        today = date.today()
        bd = birth_date.date() if isinstance(birth_date, datetime) else birth_date
        return today.year - bd.year - ((today.month, today.day) < (bd.month, bd.day))
    except Exception:
        return None