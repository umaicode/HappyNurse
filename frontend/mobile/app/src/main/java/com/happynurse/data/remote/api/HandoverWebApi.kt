// 일반 BE 인수인계 보조 API — 입퇴원 환자 목록, 체크리스트 GET/PATCH
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.HandoverChecksDataDto
import com.happynurse.data.remote.model.HandoverChecksPatchRequestDto
import com.happynurse.data.remote.model.WardEventsDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface HandoverWebApi {
    @GET("handover/ward-events")
    suspend fun getWardEvents(): Response<ApiResponse<WardEventsDataDto>>

    @GET("handover/{handoverId}")
    suspend fun getHandoverChecks(
        @Path("handoverId") handoverId: String,
    ): Response<ApiResponse<HandoverChecksDataDto>>

    // body = { "checks": { "synthesis.{idx}": Boolean } } (delta).
    @PATCH("handover/{handoverId}/checks")
    suspend fun patchHandoverChecks(
        @Path("handoverId") handoverId: String,
        @Body body: HandoverChecksPatchRequestDto,
    ): Response<ApiResponse<Unit>>
}
