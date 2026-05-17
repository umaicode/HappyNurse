// 알림함 DTO — GET /notifications/me, GET /notifications
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class NotificationListItemResponse(
    @SerializedName("notificationId") val notificationId: Long,
    @SerializedName("sourceType") val sourceType: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("patientId") val patientId: Long?,
    @SerializedName("patientName") val patientName: String?,
    @SerializedName("sourceEntityId") val sourceEntityId: Long?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("recipientPractitionerId") val recipientPractitionerId: Long?,
    @SerializedName("priority") val priority: String? = null,
)

data class NotificationListResponse(
    @SerializedName("items") val items: List<NotificationListItemResponse> = emptyList(),
    @SerializedName("nextBefore") val nextBefore: Long?,
)
