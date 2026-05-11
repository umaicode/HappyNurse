// SttAlarmReceiver — AlarmManager 가 정시에 발사하는 BroadcastReceiver.
// 화면이 켜져 있어도 풀스크린이 뜨도록 fullScreenIntent + 고우선순위 알림을 발행한다.
package com.happynurse.wear.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

class SttAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sttId = intent.getStringExtra(EXTRA_STT_ID).orEmpty()
        val patient = intent.getStringExtra(EXTRA_PATIENT).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val roomBedTime = intent.getStringExtra(EXTRA_ROOM_BED_TIME).orEmpty()

        ensureChannel(context)

        val fullIntent = Intent(context, SttAlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
            )
            putExtra(SttAlarmActivity.EXTRA_PATIENT, patient)
            putExtra(SttAlarmActivity.EXTRA_CONTENT, content)
            putExtra(SttAlarmActivity.EXTRA_ROOM_BED_TIME, roomBedTime)
        }
        val fullPi = PendingIntent.getActivity(
            context,
            sttId.hashCode(),
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("알람")
            .setContentText(content.ifBlank { "알람 시각이 되었습니다" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(notificationIdFor(sttId), notif)
    }

    companion object {
        const val CHANNEL_ID = "happynurse_stt_alarm"
        private const val CHANNEL_NAME = "STT 타이머 알람"
        private const val CHANNEL_DESC = "예약한 음성 메모 알람 — 풀스크린"

        const val EXTRA_STT_ID = "extra.stt_id"
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_CONTENT = "extra.content"
        const val EXTRA_ROOM_BED_TIME = "extra.room_bed_time"

        fun notificationIdFor(sttId: String): Int = sttId.hashCode() and 0x7FFFFFFF

        fun ensureChannel(context: Context) {
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
}
