// NFC 토큰 → 환자 진입 정보 Retrofit 인터페이스 — GET /nfc/patients/entry
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.NfcEntryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NfcTokenApi {
    @GET("nfc/patients/entry")
    suspend fun resolveByToken(@Query("token") token: String): Response<ApiResponse<NfcEntryResponse>>
}
