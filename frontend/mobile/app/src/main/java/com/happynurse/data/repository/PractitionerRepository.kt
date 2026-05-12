// 간호사(Practitioner) Repository — 본인 프로필 조회 / 담당환자 일괄 저장
package com.happynurse.data.repository

import com.happynurse.data.remote.apiCall
import com.happynurse.data.remote.api.PractitionerApi
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.data.remote.model.AssignedPatientUpdateRequest
import com.happynurse.domain.model.NurseProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PractitionerRepository @Inject constructor(
    private val api: PractitionerApi,
) {
    suspend fun getProfile(): Result<NurseProfile> =
        apiCall("프로필 조회 실패") { api.getMyProfile() }
            .map { it.toDomain() }

    suspend fun updateAssignedPatients(encounterIds: List<Long>): Result<Unit> = runCatching {
        val res = api.updateAssignedPatients(AssignedPatientUpdateRequest(encounterIds))
        val body = res.body()
        if (!(res.isSuccessful && body?.success == true)) {
            throw Exception(body?.message ?: "담당환자 저장 실패")
        }
    }
}
