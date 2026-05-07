// 병동(Ward) Retrofit 인터페이스 — 내가 속한 병동 환자 목록 조회
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.WardPatientDto
import retrofit2.Response
import retrofit2.http.GET

interface WardApi {
    @GET("wards/me/patients")
    suspend fun getMyWardPatients(): Response<ApiResponse<List<WardPatientDto>>>
}
