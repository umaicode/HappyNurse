// 백엔드 공통 응답 래퍼 — 모든 REST 응답이 success/data/errorCode 형식으로 감싸져 내려온다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val errorCode: String? = null,
    val data: T? = null,
)
