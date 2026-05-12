// /reminders/stt POST 응답 — 등록된 STT 알람의 ID/발사 시각/본문 요약.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SttReminderResponse(
    val sttReminderId: Long,
    val fireAtEpochMillis: Long? = null,
    val contentSummary: String? = null,
)
