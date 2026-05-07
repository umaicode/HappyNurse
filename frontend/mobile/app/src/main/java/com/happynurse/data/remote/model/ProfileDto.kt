// 마이페이지 프로필 DTO — GET /app/practitioners/me/profile 응답 데이터
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class AppProfileResponse(
    @SerializedName("practitionerId") val practitionerId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("employeeNumber") val employeeNumber: String,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("wardId") val wardId: Long,
    @SerializedName("wardName") val wardName: String,
    @SerializedName("organizationId") val organizationId: Long,
    @SerializedName("organizationName") val organizationName: String,
)
