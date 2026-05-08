from enum import Enum


class SeverityLevel(str, Enum):
    STABLE = "stable"
    WATCHER = "watcher"
    UNSTABLE = "unstable"


def calculate_severity(vitals: dict) -> SeverityLevel:
    """
    NEWS2 + PMC9310747 수술 후 환자 연구 기반 중증도 계산.
    vitals keys: hr, bp_sys, temp, spo2, rr (모두 선택적)
    """
    hr = vitals.get("hr")
    bp_sys = vitals.get("bp_sys")
    temp = vitals.get("temp")
    spo2 = vitals.get("spo2")
    rr = vitals.get("rr")

    # UNSTABLE: NEWS2 ≥7 상당, 즉각 개입 필요
    if hr is not None and (hr >= 131 or hr <= 40):
        return SeverityLevel.UNSTABLE
    if bp_sys is not None and (bp_sys <= 90 or bp_sys >= 220):
        return SeverityLevel.UNSTABLE
    if temp is not None and (temp >= 39.1 or temp <= 35.0):
        return SeverityLevel.UNSTABLE
    if spo2 is not None and spo2 <= 91:
        return SeverityLevel.UNSTABLE
    if rr is not None and (rr >= 25 or rr <= 8):
        return SeverityLevel.UNSTABLE

    # WATCHER: NEWS2 5-6 상당, 주의 모니터링 필요
    if hr is not None and hr >= 110:
        return SeverityLevel.WATCHER
    if bp_sys is not None and bp_sys <= 100:
        return SeverityLevel.WATCHER
    if temp is not None and (temp >= 38.1 or temp <= 36.0):
        return SeverityLevel.WATCHER
    if spo2 is not None and spo2 <= 95:
        return SeverityLevel.WATCHER
    if rr is not None and rr >= 21:
        return SeverityLevel.WATCHER

    return SeverityLevel.STABLE
