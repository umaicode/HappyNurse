// FCM 수신 서비스 — 새 토큰 등록 콜백 + 포그라운드 알림 표시
package com.happynurse.data.remote.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.happynurse.MainActivity
import com.happynurse.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class HappyNurseFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmTokenRegistrar.handleNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "알림"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Log.d(TAG, "메시지 수신: title=$title body=$body data=${message.data}")
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        NotificationChannels.ensureCreated(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.PATIENT_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        getSystemService<NotificationManager>()?.notify(Random.nextInt(), notification)
    }

    companion object {
        private const val TAG = "FCM"
    }
}
