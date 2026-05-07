// IV 수액 타이머 도메인 모델 — 홈 수액 탭 카드 / s08 진행화면 / s09 풀스크린 알람용.
// 메인 앱이 NFC 태깅으로 생성, Wearable Data Layer 로 워치에 동기화된 데이터.
package com.happynurse.wear.data.model

data class IvInfusionTimer(
    val ivInfusionId: Long,
    val patientName: String,
    val medicationName: String,
    val remainingTimeText: String,
    val remainingSec: Int,
    val totalSec: Int,
    val endAtDisplay: String,
    val room: String,
    val bedName: String,
) {
    val isUrgent: Boolean get() = remainingSec in 1..(5 * 60)
    val progress: Float get() = if (totalSec <= 0) 0f else 1f - (remainingSec.toFloat() / totalSec)
    val patientRoomBed: String get() = "$room · $bedName"
}
