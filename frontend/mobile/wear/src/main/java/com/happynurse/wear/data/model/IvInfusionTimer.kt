// 수액 타이머 도메인 모델 — 홈 수액 탭 카드 / 수액 진행 상세 / 수액 종료 알람용.
// 백엔드 /iv 응답을 매핑한 후 환자 호실/침상 정보까지 결합한 워치 표시 단위.
package com.happynurse.wear.data.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class IvInfusionTimer(
    val ivInfusionId: Long,
    val patientId: Long,
    val patientName: String,
    val medicationNames: List<String>,
    val remainingSec: Int,
    val totalSec: Int,
    val expectedEndAt: Instant?,
    val roomName: String,
    val bedName: String,
) {
    val medicationLabel: String
        get() = medicationNames.filter { it.isNotBlank() }.joinToString(", ").ifBlank { "수액" }

    val patientRoomBed: String
        get() = "$roomName-$bedName"

    val progress: Float
        get() = if (totalSec <= 0) 0f else 1f - (remainingSec.toFloat() / totalSec)

    val endAtDisplay: String
        get() = expectedEndAt
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalTime()
            ?.format(END_AT_FORMATTER)
            ?: "--:--"

    val remainingTimeText: String
        get() = formatRemaining(remainingSec)

    companion object {
        private val END_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun formatRemaining(sec: Int): String {
            if (sec <= 0) return "종료"
            val minutes = sec / 60
            val seconds = sec % 60
            return when {
                minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m 남음"
                minutes > 0 -> "${minutes}분 남음"
                else -> "${seconds}초 남음"
            }
        }
    }
}
