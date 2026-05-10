// /iv 응답 항목 — 진행 중인 수액의 환자 정보, 약물 목록, 예상 종료 시각, 남은 시간(초)을 담는다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class IvInfusionListItemResponse(
    val ivInfusionId: Long,
    val patientId: Long,
    val patientName: String? = null,
    val medicationNames: List<String> = emptyList(),
    val currentRateMlPerHr: Double? = null,
    val status: String? = null,
    val startedAt: String? = null,
    val expectedEndAt: String? = null,
    val remainingSeconds: Long? = null,
)
