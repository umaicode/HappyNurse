// STT 음성 메모 알람 Retrofit 인터페이스 — GET /reminders/stt
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.SttReminderListItemResponse
import retrofit2.Response
import retrofit2.http.GET

interface SttReminderApi {
    // 본인이 등록한 SCHEDULED 상태의 STT 알람을 fireAt 오름차순으로 반환
    @GET("reminders/stt")
    suspend fun listMine(): Response<ApiResponse<List<SttReminderListItemResponse>>>
}
