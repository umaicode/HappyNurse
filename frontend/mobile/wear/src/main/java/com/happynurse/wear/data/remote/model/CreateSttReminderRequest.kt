// /reminders/stt POST 요청 — STT 원문과 발사 시각(preview 응답값)을 함께 보내 알람을 등록한다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSttReminderRequest(
    val sttText: String,
    val fireAtEpochMillis: Long,
)
