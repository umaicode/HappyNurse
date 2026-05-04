package com.happynurse.data.remote.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRegistrar @Inject constructor() {

    fun fetchAndLog() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "토큰 발급 실패", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(TAG, "토큰 발급: $token")
                // TODO: 로그인 + JWT 저장 로직 완성 후 백엔드 등록 호출 (POST /api/devices/fcm-token)
            }
    }

    fun handleNewToken(token: String) {
        Log.d(TAG, "토큰 갱신: $token")
        // TODO: 로그인 + JWT 저장 로직 완성 후 백엔드 등록 호출 (POST /api/devices/fcm-token)
    }

    companion object {
        private const val TAG = "FCM"
    }
}
