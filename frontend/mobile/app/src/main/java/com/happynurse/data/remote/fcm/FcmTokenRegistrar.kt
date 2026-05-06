// FCM 토큰 등록 헬퍼 — Firebase 토큰 조회 + 백엔드(/devices/fcm-token) 등록
package com.happynurse.data.remote.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.happynurse.data.remote.api.FcmTokenApi
import com.happynurse.data.remote.api.FcmTokenRegisterRequest
import com.happynurse.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val api: FcmTokenApi,
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerCurrentToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> register(token) }
            .addOnFailureListener { Log.w(TAG, "토큰 발급 실패", it) }
    }

    fun handleNewToken(token: String) {
        register(token)
    }

    private fun register(token: String) {
        scope.launch {
            if (authRepository.accessToken.firstOrNull() == null) return@launch
            runCatching {
                val res = api.registerFcmToken(FcmTokenRegisterRequest(token, "mobile"))
                if (!res.isSuccessful) Log.w(TAG, "FCM 등록 실패: ${res.code()}")
            }.onFailure { Log.w(TAG, "FCM 등록 예외", it) }
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}
