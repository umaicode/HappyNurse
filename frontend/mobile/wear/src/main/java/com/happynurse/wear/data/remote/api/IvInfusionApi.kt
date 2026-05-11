// 수액 목록 조회용 Retrofit 인터페이스. 병동 식별자와 상태 필터로 진행 중 수액을 가져온다.
package com.happynurse.wear.data.remote.api

import com.happynurse.wear.data.remote.model.ApiResponse
import com.happynurse.wear.data.remote.model.IvInfusionListItemResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface IvInfusionApi {
    @GET("iv")
    suspend fun list(
        @Query("wardId") wardId: Long,
        @Query("status") status: String? = "IN_PROGRESS",
    ): ApiResponse<List<IvInfusionListItemResponse>>
}
