// PhoneDataListenerService — 워치 → 폰 Wearable Data Layer 메시지 수신 서비스.
// 워치 FCM 토큰 등록 위임과 인증 토큰 응답을 처리한다.
package com.happynurse.data.wearable

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import com.happynurse.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class PhoneDataListenerService : WearableListenerService() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var phoneDataClient: PhoneDataClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "워치 → 폰 수신: path=${messageEvent.path} bytes=${messageEvent.data.size}")
        when (messageEvent.path) {
            WearableMessagePaths.WEAR_FCM_TOKEN -> handleWearFcmToken(messageEvent.data)
            WearableMessagePaths.WEAR_AUTH_TOKEN_REQUEST -> handleAuthTokenRequest()
        }
    }

    /** 워치 FCM 토큰을 받아 백엔드 /devices/fcm-token 에 deviceType=watch 로 대행 등록. */
    private fun handleWearFcmToken(data: ByteArray) {
        val token = runCatching { String(data, Charsets.UTF_8) }.getOrNull()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "워치 FCM 토큰 디코딩 실패")
            return
        }
        Log.d(TAG, "워치 FCM 토큰 수신 → 백엔드 등록 위임")
        fcmTokenRegistrar.registerWearToken(token)
    }

    /** 워치가 시작 시 토큰을 모를 때 발송하는 요청 — DataStore 의 accessToken/wardId 를 워치에 회신. */
    private fun handleAuthTokenRequest() {
        scope.launch {
            val token = authRepository.accessToken.firstOrNull()
            val wardId = authRepository.wardId.firstOrNull()
            if (token.isNullOrBlank() || wardId == null) {
                Log.d(TAG, "워치 토큰 요청 무시 — 폰이 로그아웃 상태")
                return@launch
            }
            val payload = Json.encodeToString(
                WearAuthTokenPayload.serializer(),
                WearAuthTokenPayload(accessToken = token, wardId = wardId),
            )
            phoneDataClient.send(
                WearableMessagePaths.WEAR_AUTH_TOKEN_RESPONSE,
                payload.toByteArray(Charsets.UTF_8),
            )
        }
    }

    companion object {
        private const val TAG = "PhoneDataListener"
    }
}
