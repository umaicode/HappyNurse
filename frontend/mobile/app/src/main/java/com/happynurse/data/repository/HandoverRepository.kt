// AI 인수인계 Repository — roster-summary, 단건 조회, 생성 트리거.
// 일반 BE 보조 API — ward-events, 체크리스트 GET/PATCH.
package com.happynurse.data.repository

import com.happynurse.data.remote.api.HandoverApi
import com.happynurse.data.remote.api.HandoverWebApi
import com.happynurse.data.remote.model.HandoverChecksPatchRequestDto
import com.happynurse.data.remote.mapper.toAdmissionDomain
import com.happynurse.data.remote.mapper.toDischargeDomain
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.domain.model.HandoverChecks
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.RosterSummary
import com.happynurse.domain.model.WardEvents
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandoverRepository @Inject constructor(
    private val api: HandoverApi,
    private val webApi: HandoverWebApi,
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

    suspend fun getWardEvents(): Result<WardEvents> = runCatching {
        val res = webApi.getWardEvents()
        val body = res.body()
        val data = body?.data
        if (res.isSuccessful && body?.success == true && data != null) {
            WardEvents(
                admissions = data.admissions.map { it.toAdmissionDomain() },
                discharges = data.discharges.map { it.toDischargeDomain() },
            )
        } else {
            throw Exception(body?.message ?: "입퇴원 환자 조회 실패 (${res.code()})")
        }
    }

    suspend fun getHandoverChecks(handoverId: String): Result<HandoverChecks> = runCatching {
        val res = webApi.getHandoverChecks(handoverId)
        val body = res.body()
        val data = body?.data
        if (res.isSuccessful && body?.success == true && data != null) {
            data.toDomain(fallbackHandoverId = handoverId)
        } else {
            throw Exception(body?.message ?: "체크리스트 조회 실패 (${res.code()})")
        }
    }

    // delta: synthesis index → on/off
    suspend fun patchHandoverChecks(
        handoverId: String,
        delta: Map<Int, Boolean>,
    ): Result<Unit> = runCatching {
        if (delta.isEmpty()) return@runCatching
        val checks = delta.mapKeys { "synthesis.${it.key}" }
        val res = webApi.patchHandoverChecks(handoverId, HandoverChecksPatchRequestDto(checks = checks))
        if (!res.isSuccessful) {
            if (res.code() == 400) throw IllegalArgumentException("체크리스트 키가 유효하지 않습니다")
            throw Exception("체크리스트 업데이트 실패 (${res.code()})")
        }
    }
}
