// 담당환자 일괄 저장 DTO — PUT /practitioners/me/patients 요청/응답
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class AssignedPatientUpdateRequest(
    @SerializedName("encounterIds") val encounterIds: List<Long>,
)

data class AssignedPatientUpdateResponse(
    @SerializedName("assignedEncounterIds") val assignedEncounterIds: List<Long> = emptyList(),
    @SerializedName("releasedEncounterIds") val releasedEncounterIds: List<Long> = emptyList(),
    @SerializedName("overwroteFromOthersCount") val overwroteFromOthersCount: Int = 0,
)
