package com.happynurse.data.remote.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HappyNurseFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmTokenRegistrar.handleNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // 포그라운드 = SSE 가 우선 (spec §4.3) — 여기서는 로그만, 추후 dedup 로직 추가
        val notificationId = message.data["notificationId"]
        val sourceType = message.data["sourceType"]
        val patientId = message.data["patientId"]
        Log.d(TAG, "포그라운드 메시지: notificationId=$notificationId sourceType=$sourceType patientId=$patientId")
    }

    companion object {
        private const val TAG = "FCM"
    }
}
