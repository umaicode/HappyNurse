// 5분 전 사전 알림(s21) 시스템 알림 아이템 모델.
package com.happynurse.wear.data.model

data class NotificationItem(
    val sourceType: SourceType,
    val sourceId: String,
    val patientLabel: String,
    val contentText: String,
    val triggerTimeDisplay: String,
    val whenLabel: String = "5분 후",
) {
    enum class SourceType { STT_TIMER, IV_INFUSION }
}
