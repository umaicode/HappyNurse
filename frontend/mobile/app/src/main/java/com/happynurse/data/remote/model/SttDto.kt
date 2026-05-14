// STT (음성인식) 응답 DTO — POST /api/stt/recognize (FastAPI 응답, ApiResponse wrapper 없음)
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class SttCorrection(
    @SerializedName("original") val original: String,
    @SerializedName("corrected") val corrected: String,
    @SerializedName("type") val type: String?, // exact / fuzzy / manual
    @SerializedName("confidence") val confidence: Double?,
)

data class SttRecognizeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("nursing_record_id") val nursingRecordId: Long?,
    @SerializedName("audio_url") val audioUrl: String?,
    @SerializedName("original_text") val originalText: String?,
    @SerializedName("corrected_text") val correctedText: String?,
    @SerializedName("corrections") val corrections: List<SttCorrection> = emptyList(),
)
