// 앱 인증 API 요청/응답 DTO — swagger /app/auth/login, /app/auth/refresh, /app/auth/logout
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

// --- 공통 래퍼 ---
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("data") val data: T?,
)

// --- 로그인 ---
data class LoginRequest(
    @SerializedName("organizationId") val organizationId: Long,
    @SerializedName("wardId") val wardId: Long,
    @SerializedName("employeeNumber") val employeeNumber: String,
    @SerializedName("password") val password: String,
)

data class AppLoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("practitionerId") val practitionerId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("employeeNumber") val employeeNumber: String,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("organizationId") val organizationId: Long,
    @SerializedName("wardId") val wardId: Long,
)

// --- 토큰 갱신 ---
data class AppRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String,
)

// --- 병원/병동 목록 ---
data class OrganizationDto(
    @SerializedName("organizationId") val organizationId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("typeCode") val typeCode: String,
)

data class WardDto(
    @SerializedName("wardId") val wardId: Long,
    @SerializedName("wardName") val wardName: String,
)
