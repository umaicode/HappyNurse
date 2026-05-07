// STT 타이머 도메인 모델 — 홈 타이머 탭 카드 / s20a 상세 / s13 풀스크린 알람용.
// 워치 s11→s12 흐름에서 생성된 사용자 발화 기반 타이머.
package com.happynurse.wear.data.model

data class SttTimer(
    val sttTimerId: String,
    val patientName: String,
    val contentSummary: String,
    val remainingSec: Int,
    val endAtDisplay: String,
    val sttText: String,
    val highlightStart: Int = -1,
    val highlightEnd: Int = -1,
    val room: String,
    val bedName: String,
) {
    val isUrgent: Boolean get() = remainingSec in 1..(5 * 60)
    val patientRoomBed: String get() = "$room · $bedName"
}
