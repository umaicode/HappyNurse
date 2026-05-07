// 간호 기록 confirm Retrofit — POST /nursing-notes/{itemId}/confirm
// AI 서버는 STT 결과를 status=draft 로 INSERT 하므로, 모바일에서 사용자 확정 시 별도 confirm 필요.
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface NursingNoteApi {
    @POST("nursing-notes/{itemId}/confirm")
    suspend fun confirm(@Path("itemId") itemId: String): Response<ApiResponse<JsonElement>>
}
