// 인수인계 화면 상단 입원/퇴원 환자 도메인 모델
package com.happynurse.domain.model

data class WardEventEntry(
    val encounterId: String,
    val patientName: String,
    val location: String,       // "roomName · bedName"
    val primaryLabel: String,   // chiefComplaint > diseaseName > surgeryName 우선
    val timestamp: String?,     // 입원 = periodStart, 퇴원 = periodEnd
)

data class WardEvents(
    val admissions: List<WardEventEntry> = emptyList(),
    val discharges: List<WardEventEntry> = emptyList(),
) {
    val isEmpty: Boolean get() = admissions.isEmpty() && discharges.isEmpty()
}
