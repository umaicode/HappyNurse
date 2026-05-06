// 병원/병동 목록 조회 Retrofit 인터페이스 — 로그인 화면 드롭다운에서 사용
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.OrganizationDto
import com.happynurse.data.remote.model.WardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OrganizationApi {
    @GET("organizations")
    suspend fun listOrganizations(): Response<ApiResponse<List<OrganizationDto>>>

    @GET("organizations/{organizationId}/wards")
    suspend fun listWards(
        @Path("organizationId") organizationId: Long,
    ): Response<ApiResponse<List<WardDto>>>
}
