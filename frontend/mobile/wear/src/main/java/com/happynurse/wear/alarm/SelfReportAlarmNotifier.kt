// SelfReportAlarmNotifier — non-CRITICAL self_report 풀스크린 알림 헬퍼.
// SttAlarmReceiver 의 채널/PendingIntent/NotificationCompat 패턴을 미러링한다.
package com.happynurse.wear.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

object SelfReportAlarmNotifier {

    const val CHANNEL_ID = "happynurse_self_report_alarm"
    private const val CHANNEL_NAME = "환자요청 풀스크린"
    private const val CHANNEL_DESC = "환자 자가보고 알림 — 풀스크린"

    fun showFullScreen(
        context: Context,
        patient: String,
        room: String,
        body: String,
        priority: String,
    ) {
        ensureChannel(context)

        val fullIntent = Intent(context, SelfReportAlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
            )
            putExtra(SelfReportAlarmActivity.EXTRA_PATIENT, patient)
            putExtra(SelfReportAlarmActivity.EXTRA_ROOM, room)
            putExtra(SelfReportAlarmActivity.EXTRA_BODY, body)
            putExtra(SelfReportAlarmActivity.EXTRA_PRIORITY, priority)
        }
        val requestCode = (patient + room + body).hashCode()
        val fullPi = PendingIntent.getActivity(
            context,
            requestCode,
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("환자 요청")
            .setContentText(body.ifBlank { "환자 요청이 도착했습니다" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(requestCode and 0x7FFFFFFF, notif)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }
}
