package com.happynurse.wear.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class WearNotification(
    val title: String,
    val patientName: String,
    val roomLocation: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class NotificationType {
    IV_ALERT,
    TIMER_ALARM,
    PATIENT_CALL,
}
