// 네비게이션 라우트 상수 — 4탭 BottomNav + 모달 라우트
package com.happynurse.presentation.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val PATIENT_DETAIL = "patient_detail/{id}"
    fun patientDetail(id: String) = "patient_detail/$id"

    const val NFC_PATIENT = "nfc_patient"

    // patientId / encounterId 인자 (NFC 흐름에서만 채워짐. 다른 진입점에선 -1L 로 호출)
    const val LOG_ENTRY = "log_entry?patientId={patientId}&encounterId={encounterId}"
    fun logEntry(patientId: Long, encounterId: Long) = "log_entry?patientId=$patientId&encounterId=$encounterId"

    const val DRUG_ENTRY = "drug_entry?patientId={patientId}&encounterId={encounterId}"
    fun drugEntry(patientId: Long, encounterId: Long) = "drug_entry?patientId=$patientId&encounterId=$encounterId"

    const val IV_TIMER_SETUP = "iv_timer_setup"
}
