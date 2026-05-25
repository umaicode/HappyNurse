// WearFirebaseMessagingService — 워치 FCM 수신 진입점.
// onNewToken: 갱신 토큰을 폰에 forward.
// onMessageReceived: sourceType 별로 풀스크린 알람(IV/STT) / 시스템 트레이(self_report/사전알림) 라우팅.
package com.happynurse.wear.data.remote.fcm

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.happynurse.wear.alarm.IvAlarmActivity
import com.happynurse.wear.alarm.SelfReportAlarmActivity
import com.happynurse.wear.alarm.SttAlarmActivity
import com.happynurse.wear.domain.model.NotificationType
import com.happynurse.wear.data.eventbus.WearEventBus
import com.happynurse.wear.domain.model.WearNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class WearFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenForwarder: WearFcmTokenForwarder
    @Inject lateinit var eventBus: WearEventBus

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        tokenForwarder.forward(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            TAG,
            "★ onMessageReceived 호출됨 — from=${message.from} " +
                "messageId=${message.messageId} " +
                "notification=${message.notification?.let { "title=${it.title}, body=${it.body}" }} " +
                "data=${message.data}",
        )
        val data = message.data
        val sourceType = data["sourceType"] ?: run {
            Log.w(TAG, "sourceType 누락 — 무시: ${data}")
            return
        }
        val notificationId = data["notificationId"]
        val title = message.notification?.title ?: data["title"].orEmpty()
        val body = message.notification?.body ?: data["body"].orEmpty()
        val previewMinutes = data["previewMinutes"]?.toIntOrNull()

        if (FcmDeduplicationStore.alreadyHandled(this, notificationId)) {
            Log.d(TAG, "중복 알림 스킵: $notificationId")
            return
        }
        Log.d(TAG, "수신: sourceType=$sourceType id=$notificationId preview=$previewMinutes priority=${data["priority"]} dataKeys=${data.keys}")

        val notifId = notificationId?.toIntOrNull() ?: Random.nextInt()
        when {
            // 5분 전 사전 알림 — 풀스크린이 아니라 시스템 트레이만 (s21 형식)
            previewMinutes != null -> SystemNotificationBuilder.showTray(
                context = this,
                notificationId = notifId,
                title = title.ifBlank { "${previewMinutes}분 후 알림" },
                body = body,
                deepLinkExtras = mapOf("sourceType" to sourceType),
            )

            sourceType == "iv_alert" -> startIvAlarm(title, body, data)

            sourceType == "timer" -> startSttAlarm(title, body, data)

            sourceType == "self_report" -> {
                val priority = data["priority"].orEmpty().trim().uppercase()
                Log.d(TAG, "self_report 분기: priority='$priority'")
                // 위급(CRITICAL) — 풀스크린 알람으로 즉시 띄움.
                // 높음(HIGH) / 보통(MEDIUM) / 낮음(LOW) / 누락 — 기존대로 시스템 트레이만.
                if (priority == "CRITICAL") {
                    Log.d(TAG, "self_report 풀스크린 알람 트리거 — priority=$priority")
                    startSelfReportAlarm(title, body, priority, data)
                } else {
                    Log.d(TAG, "self_report 트레이 알림 트리거 — priority=$priority")
                    SystemNotificationBuilder.showTray(
                        context = this,
                        notificationId = notifId,
                        title = title.ifBlank { "환자 알림" },
                        body = body,
                        deepLinkExtras = mapOf(
                            "sourceType" to "self_report",
                            "patientId" to data["patientId"].orEmpty(),
                        ),
                    )
                }
                eventBus.emitNotification(
                    WearNotification(
                        title = title.ifBlank { "환자 알림" },
                        patientName = data["patientName"].orEmpty(),
                        roomLocation = data["roomLocation"].orEmpty(),
                        type = NotificationType.PATIENT_CALL,
                    ),
                )
            }

            else -> Log.w(TAG, "알 수 없는 sourceType: $sourceType")
        }
    }

    private fun startIvAlarm(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, IvAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(IvAlarmActivity.EXTRA_PATIENT, data["patientName"].orEmpty().ifBlank { title })
            putExtra(IvAlarmActivity.EXTRA_MEDICATION, data["medicationName"].orEmpty().ifBlank { body })
            putExtra(IvAlarmActivity.EXTRA_ROOM_BED_TIME, data["roomBedTime"].orEmpty())
        }
        startActivity(intent)
    }

    private fun startSelfReportAlarm(
        title: String,
        body: String,
        priority: String,
        data: Map<String, String>,
    ) {
        val intent = Intent(this, SelfReportAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SelfReportAlarmActivity.EXTRA_PATIENT, data["patientName"].orEmpty().ifBlank { title })
            putExtra(SelfReportAlarmActivity.EXTRA_ROOM, data["roomLocation"].orEmpty())
            putExtra(SelfReportAlarmActivity.EXTRA_BODY, body)
            putExtra(SelfReportAlarmActivity.EXTRA_PRIORITY, priority)
        }
        startActivity(intent)
    }

    private fun startSttAlarm(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, SttAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SttAlarmActivity.EXTRA_PATIENT, data["patientName"].orEmpty().ifBlank { title })
            putExtra(SttAlarmActivity.EXTRA_CONTENT, data["contentSummary"].orEmpty().ifBlank { body })
            putExtra(SttAlarmActivity.EXTRA_ROOM_BED_TIME, data["roomBedTime"].orEmpty())
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "WearFcm"
    }
}
