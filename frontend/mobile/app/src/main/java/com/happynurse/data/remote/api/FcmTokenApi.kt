package com.happynurse.data.remote.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FcmTokenApi {
    @POST("api/devices/fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") authorization: String,
        @Body request: FcmTokenRegisterRequest,
    ): FcmTokenRegisterResponse
}

data class FcmTokenRegisterRequest(
    val fcmToken: String,
    val deviceType: String,
)

data class FcmTokenRegisterResponse(
    val success: Boolean,
    val message: String?,
    val data: FcmTokenRegisterData?,
)

data class FcmTokenRegisterData(
    val deviceId: Long,
)
