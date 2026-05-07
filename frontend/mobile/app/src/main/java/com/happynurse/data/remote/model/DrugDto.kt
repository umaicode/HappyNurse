// 약물 투약 DTO — POST /drug/verify, /drug/record (swagger Medication*)
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

// --- /drug/verify ---
data class DrugVerifyRequest(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("tagUid") val tagUid: String,
)

data class DrugVerifyResponse(
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("medicationOrderId") val medicationOrderId: Long,
)

// --- /drug/record ---
data class DrugAdministrationSaveRequest(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("medicationOrderIds") val medicationOrderIds: List<Long>,
)

data class DrugAdministrationSaveResponse(
    @SerializedName("taggingId") val taggingId: String,
    @SerializedName("savedCount") val savedCount: Int,
    @SerializedName("medicationAdminIds") val medicationAdminIds: List<Long>,
)
