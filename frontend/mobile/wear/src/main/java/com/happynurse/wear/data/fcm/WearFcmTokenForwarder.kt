// WearFcmTokenForwarder — FirebaseMessaging.token 을 발급받아 폰에 Wearable Data Layer 로 전달.
// 폰이 자기 인증 토큰으로 백엔드 /devices/fcm-token (deviceType=watch) 에 대행 등록한다.
package com.happynurse.wear.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.happynurse.wear.data.remote.WearDataClient
import com.happynurse.wear.data.remote.WearableMessagePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearFcmTokenForwarder @Inject constructor(
    private val wearDataClient: WearDataClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 현재 토큰을 즉시 발급받아 폰에 전달. 앱 시작 시 호출. */
    fun forwardCurrentToken() {
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "현재 토큰 = $token")
                forward(token)
            }.onFailure { Log.w(TAG, "토큰 발급 실패", it) }
        }
    }

    /** onNewToken 콜백에서 호출 — 갱신된 토큰을 폰에 재전달. */
    fun forward(token: String) {
        scope.launch {
            runCatching {
                wearDataClient.send(
                    WearableMessagePaths.WEAR_FCM_TOKEN,
                    token.toByteArray(Charsets.UTF_8),
                )
                Log.d(TAG, "워치 토큰 폰 전달 성공")
            }.onFailure { Log.w(TAG, "토큰 전달 실패 (폰 미연결?)", it) }
        }
    }

    companion object {
        private const val TAG = "WearFcm"
    }
}
