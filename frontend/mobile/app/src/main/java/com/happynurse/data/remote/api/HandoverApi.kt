// AI 인수인계 Retrofit 인터페이스 — AI 서버(FastAPI), ApiResponse wrapper 없음.
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.HandoverDetailDto
import com.happynurse.data.remote.model.HandoverJobDto
import com.happynurse.data.remote.model.RosterSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HandoverApi {
    // 진입 시 담당환자 전체 시프트 요약(즉석 조립, DB 미저장)
    @GET("api/handover/roster-summary")
    suspend fun getRosterSummary(): Response<RosterSummaryDto>

    // handover_id 로 PASS-BAR 풀 리포트 단건 조회
    @GET("api/handover/{handover_id}")
    suspend fun getHandoverDetail(@Path("handover_id") handoverId: String): Response<HandoverDetailDto>

    // 담당 환자 전체 리포트 비동기 생성 → job_id 반환
    @POST("api/handover/generate")
    suspend fun generate(): Response<HandoverJobDto>

    // 단일 환자 리포트 재생성 (기존 리포트는 유지)
    @POST("api/handover/{encounter_id}/regenerate")
    suspend fun regenerate(@Path("encounter_id") encounterId: String): Response<Unit>
}
