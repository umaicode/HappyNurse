// FcmDeduplicationStore — 최근 N개 notificationId 를 SharedPreferences 에 LRU 로 저장해 중복 진입을 막는다.
// FCM 재발송 / Wearable Data Layer 잔존 경로와 동시 도착 시 한 번만 처리되도록 한다.
package com.happynurse.wear.data.remote.fcm

import android.content.Context

object FcmDeduplicationStore {
    private const val PREFS = "wear_fcm_dedup"
    private const val KEY_RECENT = "recent"
    private const val MAX = 32

    @Synchronized
    fun alreadyHandled(context: Context, notificationId: String?): Boolean {
        if (notificationId.isNullOrBlank()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString(KEY_RECENT, "").orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
        if (recent.contains(notificationId)) return true
        val updated = (listOf(notificationId) + recent).take(MAX).joinToString(",")
        prefs.edit().putString(KEY_RECENT, updated).apply()
        return false
    }
}
