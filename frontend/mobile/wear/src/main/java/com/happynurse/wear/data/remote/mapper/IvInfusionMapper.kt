// /iv 응답 + 환자 호실/침상 매핑 → 수액 타이머 도메인 모델 변환 함수.
package com.happynurse.wear.data.remote.mapper

import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.remote.model.IvInfusionListItemResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private fun parseInstantFlexible(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
}

fun IvInfusionListItemResponse.toDomain(
    roomBed: Pair<String, String>?,
    now: Instant = Instant.now(),
): IvInfusionTimer {
    val expectedEnd = parseInstantFlexible(expectedEndAt)
    val started = parseInstantFlexible(startedAt)
    val totalSec = if (started != null && expectedEnd != null) {
        Duration.between(started, expectedEnd).seconds.toInt().coerceAtLeast(0)
    } else {
        (remainingSeconds ?: 0L).toInt().coerceAtLeast(0)
    }
    val remaining = remainingSeconds?.toInt()
        ?: expectedEnd?.let { Duration.between(now, it).seconds.toInt() }
        ?: 0
    return IvInfusionTimer(
        ivInfusionId = ivInfusionId,
        patientId = patientId,
        patientName = patientName.orEmpty(),
        medicationNames = medicationNames,
        remainingSec = remaining.coerceAtLeast(0),
        totalSec = totalSec,
        expectedEndAt = expectedEnd,
        roomName = roomBed?.first.orEmpty(),
        bedName = roomBed?.second.orEmpty(),
    )
}
