// 수액 IV Retrofit 인터페이스 — POST /iv/start, GET/POST/PATCH /iv/by-tag/{tagUid}/*
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.ChangeRateRequest
import com.happynurse.data.remote.model.IvInfusionListItemResponse
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.data.remote.model.StartIvRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IvApi {
    @POST("iv/start")
    suspend fun start(@Body request: StartIvRequest): Response<ApiResponse<IvInfusionResponse>>

    @GET("iv/by-tag/{tagUid}")
    suspend fun getByTag(@Path("tagUid") tagUid: String): Response<ApiResponse<IvInfusionResponse>>

    @POST("iv/by-tag/{tagUid}/complete")
    suspend fun completeByTag(@Path("tagUid") tagUid: String): Response<ApiResponse<IvInfusionResponse>>

    @PATCH("iv/by-tag/{tagUid}/rate")
    suspend fun changeRateByTag(
        @Path("tagUid") tagUid: String,
        @Body request: ChangeRateRequest,
    ): Response<ApiResponse<IvInfusionResponse>>

    // 병동 IV 보드 — wardId 필수, status 필터 옵션
    @GET("iv")
    suspend fun getByWard(
        @Query("wardId") wardId: Long,
        @Query("status") status: String? = null,
    ): Response<ApiResponse<List<IvInfusionListItemResponse>>>
}
