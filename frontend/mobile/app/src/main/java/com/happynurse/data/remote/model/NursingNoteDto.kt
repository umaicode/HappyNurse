// 간호일지 DTO — GET /encounters/{encounterId}/nursing-notes?date= 응답 (STT_NOTE / MEDICATION 통합)
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class NursingNoteDto(
    @SerializedName("type") val type: String,
    @SerializedName("occurredAt") val occurredAt: String,
    @SerializedName("status") val status: String?,
    @SerializedName("authorPractitionerId") val authorPractitionerId: Long = 0L,
    @SerializedName("authorName") val authorName: String?,
    @SerializedName("editable") val editable: Boolean = true,
    @SerializedName("nursingRecordId") val nursingRecordId: Long? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("taggingId") val taggingId: String? = null,
    @SerializedName("nfcTagVerified") val nfcTagVerified: Boolean? = null,
    @SerializedName("medications") val medications: List<MedicationItemDto>? = null,
)

data class MedicationItemDto(
    @SerializedName("productName") val productName: String?,
    @SerializedName("dosageQuantity") val dosageQuantity: String?,
)
