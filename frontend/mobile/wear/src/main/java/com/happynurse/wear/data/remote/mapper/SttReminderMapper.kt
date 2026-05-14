// /reminders/stt 응답 → 음성메모 알람 도메인 모델 변환 함수. 남은 시간(초)은 현재 시각 기준 계산.
package com.happynurse.wear.data.remote.mapper

import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.data.remote.model.SttReminderListItemResponse
import java.time.Duration
import java.time.Instant

fun SttReminderListItemResponse.toDomain(now: Instant = Instant.now()): SttTimer {
    val fireAt = fireAtEpochMillis?.let { Instant.ofEpochMilli(it) }
    val remainingSec = fireAt
        ?.let { Duration.between(now, it).seconds.toInt() }
        ?.coerceAtLeast(0)
        ?: 0
    return SttTimer(
        sttReminderId = sttReminderId,
        contentSummary = contentSummary.orEmpty().ifBlank { "음성 메모" },
        remainingSec = remainingSec,
        fireAt = fireAt,
        sttText = sttText.orEmpty(),
    )
}
