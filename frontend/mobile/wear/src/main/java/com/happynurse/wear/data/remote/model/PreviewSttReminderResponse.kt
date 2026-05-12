// /reminders/stt/preview 응답 — 파싱된 알람 시각(epoch millis, KST).
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PreviewSttReminderResponse(
    val fireAtEpochMillis: Long? = null,
)
