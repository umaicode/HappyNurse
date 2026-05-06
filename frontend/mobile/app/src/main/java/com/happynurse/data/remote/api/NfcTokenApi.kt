package com.happynurse.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NfcTokenApi {
    @GET("nfc/patients/entry")
    suspend fun resolveByToken(@Query("token") token: String): NfcPatientEntryResponse
}

data class NfcPatientEntryResponse(
    val patientId: Long,
    val patientName: String,
    val roomName: String,
)
