// 음성 메모 알람 Retrofit 인터페이스 — 본인 알람 목록 조회, STT 텍스트 시간 파싱(preview), 알람 등록.
package com.happynurse.wear.data.remote.api

import com.happynurse.wear.data.remote.model.ApiResponse
import com.happynurse.wear.data.remote.model.CreateSttReminderRequest
import com.happynurse.wear.data.remote.model.PreviewSttReminderRequest
import com.happynurse.wear.data.remote.model.PreviewSttReminderResponse
import com.happynurse.wear.data.remote.model.SttReminderListItemResponse
import com.happynurse.wear.data.remote.model.SttReminderResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SttReminderApi {
    @GET("reminders/stt")
    suspend fun list(): ApiResponse<List<SttReminderListItemResponse>>

    @POST("reminders/stt/preview")
    suspend fun preview(
        @Body request: PreviewSttReminderRequest,
    ): ApiResponse<PreviewSttReminderResponse>

    @POST("reminders/stt")
    suspend fun create(
        @Body request: CreateSttReminderRequest,
    ): ApiResponse<SttReminderResponse>

    @DELETE("reminders/stt/{reminderId}")
    suspend fun cancel(
        @Path("reminderId") reminderId: Long,
    ): ApiResponse<Unit>
}
