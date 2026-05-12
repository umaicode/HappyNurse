// API 응답 → Result<T> 변환 헬퍼 — Repository 의 success/data 체크 보일러플레이트 제거
package com.happynurse.data.remote

import com.happynurse.data.remote.model.ApiResponse
import retrofit2.Response

// Retrofit Response<ApiResponse<T>> 호출을 1줄로 축약.
// HTTP 성공 + body.success=true + body.data≠null 일 때만 성공, 아니면 message 로 예외.
//
// 사용:
//   suspend fun getPatient(id: Long): Result<Patient> = apiCall("환자 조회 실패") {
//       patientApi.getPatient(id)
//   }.map { it.toDomain() }
suspend inline fun <T : Any> apiCall(
    errorMessage: String,
    crossinline block: suspend () -> Response<ApiResponse<T>>,
): Result<T> = runCatching {
    val res = block()
    val body = res.body()
    if (res.isSuccessful && body?.success == true && body.data != null) body.data
    else throw Exception(body?.message ?: errorMessage)
}
