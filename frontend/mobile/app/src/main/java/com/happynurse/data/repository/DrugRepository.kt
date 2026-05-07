// 약물 투약 Repository — verify(태그 단건) + saveAdministrations(검증 통과 ID 묶음)
package com.happynurse.data.repository

import com.happynurse.data.remote.api.DrugApi
import com.happynurse.data.remote.model.DrugAdministrationSaveRequest
import com.happynurse.data.remote.model.DrugAdministrationSaveResponse
import com.happynurse.data.remote.model.DrugVerifyRequest
import com.happynurse.data.remote.model.DrugVerifyResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrugRepository @Inject constructor(
    private val api: DrugApi,
) {
    suspend fun verify(patientId: Long, tagUid: String): Result<DrugVerifyResponse> = runCatching {
        val res = api.verify(DrugVerifyRequest(patientId, tagUid))
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data
        else throw Exception(body?.message ?: "약물 검증 실패")
    }

    suspend fun saveAdministrations(
        patientId: Long,
        encounterId: Long,
        medicationOrderIds: List<Long>,
    ): Result<DrugAdministrationSaveResponse> = runCatching {
        val res = api.saveAdministrations(
            DrugAdministrationSaveRequest(patientId, encounterId, medicationOrderIds),
        )
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data
        else throw Exception(body?.message ?: "투약 기록 저장 실패")
    }
}
