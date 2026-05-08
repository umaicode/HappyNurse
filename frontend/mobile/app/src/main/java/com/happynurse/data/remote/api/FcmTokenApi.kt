// FCM 디바이스 토큰 등록 API — POST /devices/fcm-token (Authorization은 OkHttp 인터셉터가 주입)
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmTokenApi {
    @POST("devices/fcm-token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRegisterRequest,
    ): Response<ApiResponse<FcmTokenRegisterData>>
}

data class FcmTokenRegisterRequest(
    val fcmToken: String,
    val deviceType: String,
)

data class FcmTokenRegisterData(
    val deviceId: Long,
)
