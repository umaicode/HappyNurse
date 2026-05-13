// API 응답 → Result<T> 변환 헬퍼 — Repository 의 success/data 체크 보일러플레이트 제거
package com.happynurse.data.remote

import com.google.gson.Gson
import com.happynurse.data.remote.model.ApiResponse
import retrofit2.Response

// Retrofit Response<ApiResponse<T>> 호출을 1줄로 축약.
// HTTP 성공 + body.success=true + body.data≠null 일 때만 성공, 아니면 message 로 예외.
// 비2xx 응답은 body() 가 null 이라 errorBody() 에서 한 번 더 message 를 살린다.
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
    if (res.isSuccessful && body?.success == true && body.data != null) {
        body.data
    } else {
        // 우선순위: body.message → errorBody 의 ApiResponse.message → fallback
        val resolved = body?.message ?: parseErrorMessage(res) ?: errorMessage
        throw Exception(resolved)
    }
}

// 비2xx 응답의 errorBody 에서 BE 가 내려준 사용자 메시지를 추출.
// 백엔드는 CustomException 핸들러에서 동일한 ApiResponse 스키마로 직렬화하므로 그대로 풀어 message 만 꺼낸다.
@PublishedApi
internal fun parseErrorMessage(res: Response<*>): String? {
    val raw = res.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        Gson().fromJson(raw, ApiResponse::class.java)?.message?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
