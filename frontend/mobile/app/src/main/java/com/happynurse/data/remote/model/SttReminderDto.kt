// STT 음성 메모 알람 DTO — GET /reminders/stt
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class SttReminderListItemResponse(
    @SerializedName("sttReminderId") val sttReminderId: Long,
    @SerializedName("contentSummary") val contentSummary: String?,
    @SerializedName("fireAtEpochMillis") val fireAtEpochMillis: Long?,
    @SerializedName("sttText") val sttText: String?,
)
