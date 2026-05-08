// 워치 화면 퍼블리싱용 mock 데이터 — 백엔드 연동 전 ViewModel 의 초기 StateFlow 시드.
// 실제 데이터로 교체 시 이 파일은 삭제하거나 Preview 전용으로 남겨도 된다.
package com.happynurse.wear.data.model

object MockData {
    val ivList: List<IvInfusionTimer> = listOf(
        IvInfusionTimer(
            ivInfusionId = 1L,
            patientName = "최현웅",
            medicationName = "N/S 1L",
            remainingTimeText = "6h 12m 남음",
            remainingSec = 22_320,
            totalSec = 28_800,
            endAtDisplay = "02:32",
            room = "702호",
            bedName = "1번",
        ),
        IvInfusionTimer(
            ivInfusionId = 2L,
            patientName = "박은지",
            medicationName = "5% D/W 500ml",
            remainingTimeText = "3분 남음",
            remainingSec = 180,
            totalSec = 14_400,
            endAtDisplay = "14:50",
            room = "702호",
            bedName = "2번",
        ),
    )

    val sttList: List<SttTimer> = listOf(
        SttTimer(
            sttTimerId = "stt-1",
            patientName = "김가민",
            contentSummary = "V/S",
            remainingSec = 1080,
            endAtDisplay = "14:55",
            sttText = "30분 후 V/S 재측정 부탁드려요",
            highlightStart = 0,
            highlightEnd = 4,
            room = "701호",
            bedName = "1번",
        ),
        SttTimer(
            sttTimerId = "stt-2",
            patientName = "이한나",
            contentSummary = "체위 변경",
            remainingSec = 240,
            endAtDisplay = "14:51",
            sttText = "4분 뒤 체위 변경",
            highlightStart = 0,
            highlightEnd = 3,
            room = "703호",
            bedName = "3번",
        ),
    )

    val reqList: List<PatientSelfReport> = listOf(
        PatientSelfReport(
            selfReportId = 11L,
            patientName = "김영자",
            symptomType = SymptomType.PAIN,
            symptomText = "허리 통증이 심해요",
            submittedRelative = "2분 전",
            room = "701호",
            bedName = "2번",
        ),
        PatientSelfReport(
            selfReportId = 12L,
            patientName = "장도현",
            symptomType = SymptomType.IV,
            symptomText = "수액이 다 떨어진 것 같아요",
            submittedRelative = "방금",
            room = "704호",
            bedName = "1번",
        ),
    )
}
