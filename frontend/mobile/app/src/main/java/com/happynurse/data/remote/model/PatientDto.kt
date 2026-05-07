// 환자 관련 DTO — 병동 환자 목록(GET /wards/me/patients)과 환자 단건(GET /patient/{id}) 응답
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class WardPatientDto(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("roomName") val roomName: String?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("unconfirmedNursingCount") val unconfirmedNursingCount: Int = 0,
    @SerializedName("isMyPatient") val isMyPatient: Boolean = false,
    @SerializedName("assignedNurseName") val assignedNurseName: String? = null,
    @SerializedName("chiefComplaint")    val chiefComplaint: String? = null,
)

data class PatientDetailDto(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("identifierValue") val identifierValue: String?,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("status") val status: String?,
    @SerializedName("periodStart") val periodStart: String?,
    @SerializedName("diseaseName") val diseaseName: String?,
    @SerializedName("chiefComplaint") val chiefComplaint: String?,
    @SerializedName("surgeryName") val surgeryName: String?,
    @SerializedName("departmentCode") val departmentCode: String?,
    @SerializedName("wardName") val wardName: String?,
    @SerializedName("roomName") val roomName: String?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("attendingPhysicianId") val attendingPhysicianId: Long = 0L,
    @SerializedName("attendingPhysicianName") val attendingPhysicianName: String?,
)
