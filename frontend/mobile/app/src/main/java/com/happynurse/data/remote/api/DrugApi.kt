// 약물 투약 Retrofit 인터페이스 — POST /drug/verify, /drug/record
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.DrugAdministrationSaveRequest
import com.happynurse.data.remote.model.DrugAdministrationSaveResponse
import com.happynurse.data.remote.model.DrugVerifyRequest
import com.happynurse.data.remote.model.DrugVerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DrugApi {
    @POST("drug/verify")
    suspend fun verify(@Body request: DrugVerifyRequest): Response<ApiResponse<DrugVerifyResponse>>

    @POST("drug/record")
    suspend fun saveAdministrations(
        @Body request: DrugAdministrationSaveRequest,
    ): Response<ApiResponse<DrugAdministrationSaveResponse>>
}
