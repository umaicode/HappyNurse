// 음성 메모 알람 저장소 — 본인 알람 목록 조회 + STT 텍스트 시간 파싱(preview) + 알람 등록.
package com.happynurse.wear.data.repository

import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.data.remote.api.SttReminderApi
import com.happynurse.wear.data.remote.mapper.toDomain
import com.happynurse.wear.data.remote.model.CreateSttReminderRequest
import com.happynurse.wear.data.remote.model.PreviewSttReminderRequest
import com.happynurse.wear.data.remote.model.SttReminderResponse
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttReminderRepository @Inject constructor(
    private val sttReminderApi: SttReminderApi,
) {
    suspend fun fetch(now: Instant = Instant.now()): Result<List<SttTimer>> = runCatching {
        val response = sttReminderApi.list()
        if (!response.success) {
            error(response.message ?: "음성 메모 알람 조회 실패")
        }
        response.data.orEmpty().map { it.toDomain(now = now) }
    }

    suspend fun previewFireAt(sttText: String): Result<Long> = runCatching {
        val response = sttReminderApi.preview(PreviewSttReminderRequest(sttText = sttText))
        if (!response.success) {
            error(response.message ?: "시간 파싱에 실패했어요")
        }
        response.data?.fireAtEpochMillis ?: error("시간 표현을 인식하지 못했어요")
    }

    suspend fun create(sttText: String, fireAtEpochMillis: Long): Result<SttReminderResponse> = runCatching {
        val response = sttReminderApi.create(
            CreateSttReminderRequest(sttText = sttText, fireAtEpochMillis = fireAtEpochMillis),
        )
        if (!response.success) {
            error(response.message ?: "알람 등록에 실패했어요")
        }
        response.data ?: error("등록 응답이 비어 있어요")
    }

    suspend fun cancel(reminderId: Long): Result<Unit> = runCatching {
        val response = sttReminderApi.cancel(reminderId)
        if (!response.success) {
            error(response.message ?: "알람 취소에 실패했어요")
        }
        Unit
    }
}
