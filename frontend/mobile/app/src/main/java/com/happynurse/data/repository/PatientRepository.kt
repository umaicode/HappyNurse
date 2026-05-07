// 환자(Patient) Repository — 병동 환자 목록 / 환자 단건 조회
package com.happynurse.data.repository

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
    suspend fun getMyWardPatients(): Result<List<Patient>> = runCatching {
        val res = wardApi.getMyWardPatients()
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.map { it.toDomain() }
        else throw Exception(body?.message ?: "병동 환자 조회 실패 (${res.code()})")
    }

    suspend fun getPatient(patientId: Long): Result<Patient> = runCatching {
        val res = patientApi.getPatient(patientId)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.toDomain()
        else throw Exception(body?.message ?: "환자 조회 실패 (${res.code()})")
    }
}
