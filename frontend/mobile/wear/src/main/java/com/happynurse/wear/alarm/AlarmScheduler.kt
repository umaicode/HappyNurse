// AlarmScheduler — AlarmManager 기반 정시 트리거 스켈레톤. 5분 전 사전 알림(s21)/만료 알람(s09/s13) 등록.
// 실제 호출부는 메인 앱 동기화 시점에 ViewModel 에서 연결 예정.
package com.happynurse.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmScheduler(private val context: Context) {

    fun scheduleIvEndAlarm(triggerAtMillis: Long, ivId: Long, patient: String, medication: String, roomBedTime: String) {
        val intent = Intent(context, IvAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(IvAlarmActivity.EXTRA_PATIENT, patient)
            putExtra(IvAlarmActivity.EXTRA_MEDICATION, medication)
            putExtra(IvAlarmActivity.EXTRA_ROOM_BED_TIME, roomBedTime)
        }
        val pi = PendingIntent.getActivity(
            context, ivId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }

    fun scheduleSttAlarm(triggerAtMillis: Long, sttId: String, patient: String, content: String, roomBedTime: String) {
        // 채널 미리 등록 — 화면 켜져있을 때도 풀스크린이 뜨려면 IMPORTANCE_HIGH 가 필요
        SttAlarmReceiver.ensureChannel(context)
        val pi = PendingIntent.getBroadcast(
            context, sttId.hashCode(), sttBroadcastIntent(sttId, patient, content, roomBedTime),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }

    fun cancelSttAlarm(sttId: String) {
        val pi = PendingIntent.getBroadcast(
            context, sttId.hashCode(), sttBroadcastIntent(sttId, "", "", ""),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        am.cancel(pi)
    }

    private fun sttBroadcastIntent(sttId: String, patient: String, content: String, roomBedTime: String) =
        Intent(context, SttAlarmReceiver::class.java).apply {
            putExtra(SttAlarmReceiver.EXTRA_STT_ID, sttId)
            putExtra(SttAlarmReceiver.EXTRA_PATIENT, patient)
            putExtra(SttAlarmReceiver.EXTRA_CONTENT, content)
            putExtra(SttAlarmReceiver.EXTRA_ROOM_BED_TIME, roomBedTime)
        }

    /** 5분 전 사전 알림 BroadcastReceiver 의 트리거 등록은 SystemNotifBuilder 와 함께 추후 연결. */
    fun cancel(requestCode: Int) {
        val pi = PendingIntent.getBroadcast(
            context, requestCode, Intent(context, PreAlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        am.cancel(pi)
    }
}

/** 사전 알림(s21) 트리거 수신용 BroadcastReceiver — TODO: 실제 알림 빌드는 SystemNotifBuilder 연결. */
class PreAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO: NotificationCompat.Builder 로 s21 트레이 알림 발송
    }
}
