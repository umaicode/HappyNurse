// 음성메모 알람(STT 리마인더) 도메인 모델 — 홈 타이머 탭 카드 / 상세 / 풀스크린 알람용.
// 백엔드 /reminders/stt 응답을 매핑한 단위 (환자/호실 정보는 응답에 없음).
package com.happynurse.wear.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SttTimer(
    val sttReminderId: Long,
    val contentSummary: String,
    val remainingSec: Int,
    val fireAt: Instant?,
    val sttText: String,
) {
    val sttTimerId: String get() = sttReminderId.toString()

    val endAtDisplay: String
        get() = fireAt
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalTime()
            ?.format(FIRE_AT_FORMATTER)
            ?: "--:--"

    val remainingTimeText: String
        get() = IvInfusionTimer.formatRemaining(remainingSec)

    companion object {
        private val FIRE_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
