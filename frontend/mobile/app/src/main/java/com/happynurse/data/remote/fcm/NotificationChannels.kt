// FCM 알림 채널 등록 — Android 8 이상 필수, 앱 시작 시 1회 호출
package com.happynurse.data.remote.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val PATIENT_ALERTS_ID = "patient_alerts"
    const val WEB_SESSION_ID = "web_session"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        val patientChannel = NotificationChannel(
            PATIENT_ALERTS_ID,
            "환자 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "환자 자가보고/호출 등 즉시 확인이 필요한 알림"
            enableVibration(true)
        }
        manager.createNotificationChannel(patientChannel)

        val sessionChannel = NotificationChannel(
            WEB_SESSION_ID,
            "웹 로그인/로그아웃",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "본인 계정의 웹 로그인/로그아웃 알림"
            enableVibration(false)
        }
        manager.createNotificationChannel(sessionChannel)
    }
}
