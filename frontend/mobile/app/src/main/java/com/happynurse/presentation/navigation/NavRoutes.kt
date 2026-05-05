// 네비게이션 라우트 상수 — 4탭 BottomNav + 모달 라우트
package com.happynurse.presentation.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val PATIENT_DETAIL = "patient_detail/{id}"
    fun patientDetail(id: String) = "patient_detail/$id"

    const val NFC_PATIENT = "nfc_patient"
    const val LOG_ENTRY = "log_entry"
    const val DRUG_ENTRY = "drug_entry"
    const val IV_TIMER_SETUP = "iv_timer_setup"
}
