// 앱 인증 Retrofit 인터페이스 — /app/auth/login, /app/auth/refresh, /app/auth/logout
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.AppLoginResponse
import com.happynurse.data.remote.model.AppRefreshRequest
import com.happynurse.data.remote.model.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("app/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AppLoginResponse>>

    @POST("app/auth/refresh")
    suspend fun refresh(@Body request: AppRefreshRequest): Response<ApiResponse<AppLoginResponse>>

    @POST("app/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
