// /reminders/stt/preview 요청 — STT 원문에서 fireAt 만 파싱(저장 X) 받기 위한 본문.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PreviewSttReminderRequest(
    val sttText: String,
)
