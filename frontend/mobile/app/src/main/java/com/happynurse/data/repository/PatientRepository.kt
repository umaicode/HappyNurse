// 환자(Patient) Repository — 병동 환자 목록 / 환자 단건 조회
package com.happynurse.data.repository

import com.happynurse.data.remote.apiCall
import com.happynurse.data.remote.api.PatientApi
import com.happynurse.data.remote.api.WardApi
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.domain.model.Patient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val wardApi: WardApi,
    private val patientApi: PatientApi,
) {
    suspend fun getMyWardPatients(): Result<List<Patient>> =
        apiCall("병동 환자 조회 실패") { wardApi.getMyWardPatients() }
            .map { list -> list.map { it.toDomain() } }

    suspend fun getPatient(patientId: Long): Result<Patient> =
        apiCall("환자 조회 실패") { patientApi.getPatient(patientId) }
            .map { it.toDomain() }
}
