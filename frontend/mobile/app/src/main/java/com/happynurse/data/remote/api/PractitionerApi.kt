// 간호사(Practitioner) Retrofit 인터페이스 — 본인 프로필 조회 / 담당환자 일괄 저장
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.AppProfileResponse
import com.happynurse.data.remote.model.AssignedPatientUpdateRequest
import com.happynurse.data.remote.model.AssignedPatientUpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface PractitionerApi {
    @GET("app/practitioners/me/profile")
    suspend fun getMyProfile(): Response<ApiResponse<AppProfileResponse>>

    @PUT("practitioners/me/patients")
    suspend fun updateAssignedPatients(
        @Body request: AssignedPatientUpdateRequest,
    ): Response<ApiResponse<AssignedPatientUpdateResponse>>
}
