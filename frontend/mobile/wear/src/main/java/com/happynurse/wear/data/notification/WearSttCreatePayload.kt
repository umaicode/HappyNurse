// 워치에서 등록하는 STT 타이머를 폰에 전달하는 페이로드.
// 폰이 백엔드 timer/reminder API 에 등록 후 만료 시 FCM(sourceType=timer)으로 워치에 도달.
package com.happynurse.wear.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class WearSttCreatePayload(
    val patientId: Long? = null,
    val patientName: String,
    val sttText: String,
    val contentSummary: String,
    val targetEpochMillis: Long,
)
