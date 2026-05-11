// AI 서버 /api/stt/recognize 응답 — 음성 인식 원문과 보정된 텍스트, 보정 이력을 담는다.
// 서버 필드는 snake_case 라 SerialName 으로 매핑한다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SttRecognizeResponse(
    val success: Boolean = false,
    @SerialName("nursing_record_id") val nursingRecordId: Long? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("original_text") val originalText: String? = null,
    @SerialName("corrected_text") val correctedText: String? = null,
    val corrections: List<SttCorrection> = emptyList(),
)

@Serializable
data class SttCorrection(
    val original: String? = null,
    val corrected: String? = null,
    val type: String? = null,
    val confidence: Double? = null,
)
