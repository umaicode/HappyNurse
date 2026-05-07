// SystemNotifBuilder — 워치 시스템 트레이 알림 발송(s21 사전 알림 / self_report 환자요청 / 풀스크린 fallback).
// 단일 NotificationChannel 'happynurse_alerts' 사용. WearMainActivity 로 PendingIntent 연결.
package com.happynurse.wear.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.happynurse.wear.WearMainActivity

object SystemNotifBuilder {

    const val CHANNEL_ID = "happynurse_alerts"
    private const val CHANNEL_NAME = "HappyNurse 알림"
    private const val CHANNEL_DESC = "수액 / STT / 환자 자가증상 알림"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun showTray(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        smallIconRes: Int = android.R.drawable.ic_dialog_info,
        deepLinkExtras: Map<String, String> = emptyMap(),
    ) {
        ensureChannel(context)
        val intent = Intent(context, WearMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            deepLinkExtras.forEach { (k, v) -> putExtra(k, v) }
        }
        val pi = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        context.getSystemService<NotificationManager>()?.notify(notificationId, notif)
    }
}
