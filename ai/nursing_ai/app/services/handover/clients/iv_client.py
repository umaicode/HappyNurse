import logging
from dataclasses import dataclass
from datetime import datetime, timedelta

import httpx

logger = logging.getLogger(__name__)
SHIFT_WINDOW_HR = 8


@dataclass
class IvStatus:
    iv_infusion_id: int
    medication_label: str
    total_volume_ml: float
    current_rate_ml_per_hr: float
    started_at: datetime
    status: str  # "active" | "paused" | "completed"


@dataclass
class IvRemaining:
    iv_infusion_id: int
    medication_label: str
    remaining_ml: float
    expected_end: datetime
    ends_in_shift: bool


def calculate_iv_remaining(iv: IvStatus, *, as_of: datetime) -> IvRemaining | None:
    """active 상태가 아니거나 속도 0이면 None 반환."""
    if iv.status != "active":
        return None
    if iv.current_rate_ml_per_hr <= 0:
        return None

    elapsed_hr = (as_of - iv.started_at).total_seconds() / 3600
    remaining_ml = iv.total_volume_ml - iv.current_rate_ml_per_hr * elapsed_hr
    remaining_ml = max(remaining_ml, 0.0)

    remaining_hr = remaining_ml / iv.current_rate_ml_per_hr
    expected_end = as_of + timedelta(hours=remaining_hr)
    ends_in_shift = timedelta(0) < (expected_end - as_of) <= timedelta(hours=SHIFT_WINDOW_HR)

    return IvRemaining(
        iv_infusion_id=iv.iv_infusion_id,
        medication_label=iv.medication_label,
        remaining_ml=round(remaining_ml, 1),
        expected_end=expected_end,
        ends_in_shift=ends_in_shift,
    )


class IvClient:
    """Spring Boot API에서 active IvInfusion 조회."""

    def __init__(self, base_url: str, timeout: float = 5.0):
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    async def get_active_infusions(self, encounter_id: str) -> list[IvStatus]:
        url = f"{self._base_url}/iv-infusions"
        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                resp = await client.get(url, params={"encounterId": encounter_id, "status": "active"})
                resp.raise_for_status()
                data = resp.json()
        except Exception as e:
            logger.warning("IV 조회 실패 [encounter=%s]: %s", encounter_id, e)
            return []

        results = []
        for item in data.get("infusions", []):
            try:
                started_at = datetime.fromisoformat(item["startedAt"])
                results.append(IvStatus(
                    iv_infusion_id=item["ivInfusionId"],
                    medication_label=item.get("medicationLabel", "수액"),
                    total_volume_ml=float(item["totalVolumeMl"]),
                    current_rate_ml_per_hr=float(item["currentRateMlPerHr"]),
                    started_at=started_at,
                    status=item.get("status", "active"),
                ))
            except (KeyError, ValueError) as e:
                logger.warning("IV 항목 파싱 실패: %s", e)
        return results
