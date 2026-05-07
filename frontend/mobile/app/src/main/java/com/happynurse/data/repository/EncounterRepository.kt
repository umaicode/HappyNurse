// 입원(Encounter) Repository — 간호일지(날짜별) / 의사오더 조회
package com.happynurse.data.repository

import com.happynurse.data.remote.api.EncounterApi
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncounterRepository @Inject constructor(
    private val api: EncounterApi,
) {
    suspend fun getNursingNotes(encounterId: Long, date: String): Result<List<Note>> = runCatching {
        val res = api.getNursingNotes(encounterId, date)
        val body = res.body()
        if (res.isSuccessful && body?.success == true) body.data.orEmpty().map { it.toDomain() }
        else throw Exception(body?.message ?: "간호일지 조회 실패 (${res.code()})")
    }

    suspend fun getOrders(encounterId: Long): Result<List<Order>> = runCatching {
        val res = api.getOrders(encounterId)
        val body = res.body()
        if (res.isSuccessful && body?.success == true) body.data?.orders.orEmpty().map { it.toDomain() }
        else throw Exception(body?.message ?: "의사오더 조회 실패 (${res.code()})")
    }
}
