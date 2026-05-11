// AI 인수인계 Repository — roster-summary, 단건 조회, 생성 트리거.
package com.happynurse.data.repository

import com.happynurse.data.remote.api.HandoverApi
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.RosterSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandoverRepository @Inject constructor(
    private val api: HandoverApi,
) {
    suspend fun getRosterSummary(): Result<RosterSummary> = runCatching {
        val res = api.getRosterSummary()
        val body = res.body()
        if (res.isSuccessful && body != null) body.toDomain()
        else throw Exception("인수인계 요약 조회 실패 (${res.code()})")
    }

    suspend fun getHandoverDetail(handoverId: String): Result<HandoverDetail> = runCatching {
        val res = api.getHandoverDetail(handoverId)
        val body = res.body()
        if (res.isSuccessful && body != null) body.toDomain(handoverId)
        else throw Exception("인수인계 상세 조회 실패 (${res.code()})")
    }

    suspend fun generate(): Result<String> = runCatching {
        val res = api.generate()
        val body = res.body()
        if (res.isSuccessful && body?.jobId != null) body.jobId
        else throw Exception("인수인계 생성 요청 실패 (${res.code()})")
    }
}
