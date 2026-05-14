// /reminders/stt 응답 항목 — 음성메모 알람의 본문 요약, 발화 원문, 발사 시각(epoch millis)을 담는다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SttReminderListItemResponse(
    val sttReminderId: Long,
    val contentSummary: String? = null,
    val fireAtEpochMillis: Long? = null,
    val sttText: String? = null,
)
