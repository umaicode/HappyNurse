// 수액 IV DTO — POST /iv/start, GET /iv/by-tag/{tagUid}, etc.
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

// --- /iv/start ---
data class StartIvRequest(
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("medicationOrderIds") val medicationOrderIds: List<Long>,
    @SerializedName("totalVolumeMl") val totalVolumeMl: Double,
    @SerializedName("rateGttPerMin") val rateGttPerMin: Int,
    @SerializedName("patientType") val patientType: String, // "ADULT" | "PEDIATRIC"
    @SerializedName("note") val note: String? = null,
)

// --- IvInfusionResponse — start / by-tag / complete / change-rate 공통 ---
data class IvInfusionResponse(
    @SerializedName("ivInfusionId") val ivInfusionId: Long,
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("patientName") val patientName: String?,
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("medicationOrderId") val medicationOrderId: Long,
    @SerializedName("medications") val medications: List<MedicationItem>,
    @SerializedName("practitionerId") val practitionerId: Long?,
    @SerializedName("totalVolumeMl") val totalVolumeMl: Double,
    @SerializedName("currentRateMlPerHr") val currentRateMlPerHr: Double,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("expectedEndAt") val expectedEndAt: String?,
    @SerializedName("actualEndAt") val actualEndAt: String?,
    @SerializedName("status") val status: String, // IN_PROGRESS|PAUSED|COMPLETED|CANCELLED|EXPIRED
    @SerializedName("note") val note: String?,
    @SerializedName("remainingVolumeMl") val remainingVolumeMl: Double?,
    @SerializedName("remainingSeconds") val remainingSeconds: Long?,
)

data class MedicationItem(
    @SerializedName("medicationId") val medicationId: Long,
    @SerializedName("medicationName") val medicationName: String,
    @SerializedName("medicationOrderId") val medicationOrderId: Long,
    @SerializedName("sequence") val sequence: Int,
)

// --- /iv/by-tag/{tagUid}/rate ---
data class ChangeRateRequest(
    @SerializedName("rateGttPerMin") val rateGttPerMin: Int,
    @SerializedName("patientType") val patientType: String, // "ADULT" | "PEDIATRIC"
)
