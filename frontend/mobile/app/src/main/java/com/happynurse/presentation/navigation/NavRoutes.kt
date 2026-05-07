// 네비게이션 라우트 상수 — 4탭 BottomNav + 모달 라우트
package com.happynurse.presentation.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val PATIENT_DETAIL = "patient_detail/{id}"
    fun patientDetail(id: String) = "patient_detail/$id"

    // optional token 쿼리 인자 — Path A (manifest dispatch) 에서 onNewIntent 가 token 을 실어 navigate
    const val NFC_PATIENT = "nfc_patient?token={token}"
    fun nfcPatient(token: String? = null) = if (token != null) "nfc_patient?token=$token" else "nfc_patient"

    // patientId / encounterId 인자 (NFC 흐름에서만 채워짐. 다른 진입점에선 -1L 로 호출)
    const val LOG_ENTRY = "log_entry?patientId={patientId}&encounterId={encounterId}"
    fun logEntry(patientId: Long, encounterId: Long) = "log_entry?patientId=$patientId&encounterId=$encounterId"

    const val DRUG_ENTRY = "drug_entry?patientId={patientId}&encounterId={encounterId}"
    fun drugEntry(patientId: Long, encounterId: Long) = "drug_entry?patientId=$patientId&encounterId=$encounterId"

    // IV setup — verify 통과한 처방 PK 콤마 인코딩 + encounterId
    const val IV_TIMER_SETUP = "iv_timer_setup?encounterId={encounterId}&orderIds={orderIds}"
    fun ivTimerSetup(encounterId: Long, orderIds: List<Long>) =
        "iv_timer_setup?encounterId=$encounterId&orderIds=${orderIds.joinToString(",")}"

    // IV active — ivInfusionId 있으면 캐시 hit (start 직후), 없으면 NFC 재태깅 진입
    const val IV_TIMER_ACTIVE = "iv_timer_active?ivInfusionId={ivInfusionId}"
    fun ivTimerActive(ivInfusionId: Long = -1L) =
        if (ivInfusionId > 0L) "iv_timer_active?ivInfusionId=$ivInfusionId" else "iv_timer_active"
}
