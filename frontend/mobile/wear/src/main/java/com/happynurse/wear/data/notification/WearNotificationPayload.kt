package com.happynurse.wear.data.notification

import kotlinx.serialization.Serializable

// 폰 → 워치로 직렬화 전송되는 알림 페이로드. type 은 메시지 path 로 결정되므로 별도 필드 없음
@Serializable
data class WearNotificationPayload(
    val title: String,
    val patientName: String,
    val roomLocation: String,
)
