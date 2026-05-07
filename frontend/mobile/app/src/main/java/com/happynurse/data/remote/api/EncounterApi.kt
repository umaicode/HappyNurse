// 입원(Encounter) Retrofit 인터페이스 — 간호일지(날짜별) / 의사오더 조회
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.NursingNoteDto
import com.happynurse.data.remote.model.OrderListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EncounterApi {
    @GET("encounters/{encounterId}/nursing-notes")
    suspend fun getNursingNotes(
        @Path("encounterId") encounterId: Long,
        @Query("date") date: String,
    ): Response<ApiResponse<List<NursingNoteDto>>>

    @GET("encounters/{encounterId}/orders")
    suspend fun getOrders(
        @Path("encounterId") encounterId: Long,
    ): Response<ApiResponse<OrderListResponse>>
}
