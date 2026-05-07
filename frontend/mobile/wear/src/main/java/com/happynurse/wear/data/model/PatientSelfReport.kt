// 환자 자가증상 보고 도메인 모델 — 홈 환자알림 탭 카드용.
// 환자가 메인 앱/태블릿으로 입력한 증상이 워치로 푸시된다.
package com.happynurse.wear.data.model

data class PatientSelfReport(
    val selfReportId: Long,
    val patientName: String,
    val symptomType: SymptomType,
    val symptomText: String,
    val submittedRelative: String,
    val room: String,
    val bedName: String,
) {
    val patientRoomBed: String get() = "$room · $bedName"
}
