// 환자(Patient) Retrofit 인터페이스 — 환자 단건 상세 조회
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.PatientDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PatientApi {
    @GET("patient/{patientId}")
    suspend fun getPatient(@Path("patientId") patientId: Long): Response<ApiResponse<PatientDetailDto>>
}
