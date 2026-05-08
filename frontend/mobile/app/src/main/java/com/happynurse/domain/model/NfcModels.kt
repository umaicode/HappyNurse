// NFC 도메인 모델 — wristband 태깅 결과 표시용
package com.happynurse.domain.model

data class NfcPatientInfo(
    val patientId: Long,
    val encounterId: Long,
    val patientName: String,
    val roomName: String?,
    val diseaseName: String?,
    val chiefComplaint: String?,
    val surgeryName: String?,
    val attendingPhysicianName: String?,
)
