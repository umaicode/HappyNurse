// 간호 기록 Repository — STT 결과 draft → confirmed 전환
package com.happynurse.data.repository

import com.happynurse.data.remote.api.NursingNoteApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NursingNoteRepository @Inject constructor(
    private val api: NursingNoteApi,
) {
    suspend fun confirm(nursingRecordId: Long): Result<Unit> = runCatching {
        val res = api.confirm(nursingRecordId.toString())
        val body = res.body()
        if (res.isSuccessful && body?.success == true) Unit
        else throw Exception(body?.message ?: "간호 기록 확정 실패")
    }
}
