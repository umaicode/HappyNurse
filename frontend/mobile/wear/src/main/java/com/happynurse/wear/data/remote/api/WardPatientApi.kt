// 병동 입원 환자 목록 조회용 Retrofit 인터페이스. patientId → 호실/침상 매핑에 사용한다.
package com.happynurse.wear.data.remote.api

import com.happynurse.wear.data.remote.model.ApiResponse
import com.happynurse.wear.data.remote.model.WardPatientListResponse
import retrofit2.http.GET

interface WardPatientApi {
    @GET("wards/me/patients")
    suspend fun list(): ApiResponse<List<WardPatientListResponse>>
}
