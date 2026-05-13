// 일반 BE /handover/ward-events 응답 DTO — 오늘 병동 입원/퇴원 환자 목록
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class WardEventEntryDto(
    @SerializedName("encounterId") val encounterId: Long? = null,
    @SerializedName("patientName") val patientName: String? = null,
    @SerializedName("roomName") val roomName: String? = null,
    @SerializedName("bedName") val bedName: String? = null,
    @SerializedName("classCode") val classCode: String? = null,
    @SerializedName("chiefComplaint") val chiefComplaint: String? = null,
    @SerializedName("diseaseName") val diseaseName: String? = null,
    @SerializedName("surgeryName") val surgeryName: String? = null,
    @SerializedName("periodStart") val periodStart: String? = null,
    @SerializedName("periodEnd") val periodEnd: String? = null,
)

data class WardEventsDataDto(
    @SerializedName("admissions") val admissions: List<WardEventEntryDto> = emptyList(),
    @SerializedName("discharges") val discharges: List<WardEventEntryDto> = emptyList(),
)
