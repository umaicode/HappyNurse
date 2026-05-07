// NFC 진입 응답 DTO — GET /nfc/patients/entry?token=
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class NfcEntryResponse(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("patientName") val patientName: String,
    @SerializedName("roomName") val roomName: String,
)
