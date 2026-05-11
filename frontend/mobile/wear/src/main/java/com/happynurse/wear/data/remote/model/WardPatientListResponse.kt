// /wards/me/patients 응답 항목 — patientId 를 호실명/침상명으로 매핑하기 위해 사용한다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class WardPatientListResponse(
    val patientId: Long,
    val encounterId: Long? = null,
    val name: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val roomName: String? = null,
    val bedName: String? = null,
    val unconfirmedNursingCount: Long? = null,
    val isMyPatient: Boolean? = null,
)
