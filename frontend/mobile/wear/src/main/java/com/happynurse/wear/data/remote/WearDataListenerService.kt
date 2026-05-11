// 폰 → 워치 메시지 수신 서비스. 수신한 페이로드를 WearEventBus 로 emit 해 ViewModel 이 collect
package com.happynurse.wear.data.remote

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.happynurse.wear.data.notification.NotificationType
import com.happynurse.wear.data.notification.WearAuthTokenPayload
import com.happynurse.wear.data.notification.WearEventBus
import com.happynurse.wear.data.notification.WearNotification
import com.happynurse.wear.data.notification.WearNotificationPayload
import com.happynurse.wear.data.auth.WearTokenStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    @Inject lateinit var eventBus: WearEventBus
    @Inject lateinit var tokenStore: WearTokenStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        android.util.Log.d(
            "WearListener",
            "onMessageReceived path=${messageEvent.path} size=${messageEvent.data.size}",
        )
        when (messageEvent.path) {
            WearableMessagePaths.IV_ALERT ->
                handleNotification(messageEvent.data, NotificationType.IV_ALERT)
            WearableMessagePaths.TIMER_ALARM ->
                handleNotification(messageEvent.data, NotificationType.TIMER_ALARM)
            WearableMessagePaths.PATIENT_CALL ->
                handleNotification(messageEvent.data, NotificationType.PATIENT_CALL)
            WearableMessagePaths.WEAR_AUTH_TOKEN_RESPONSE ->
                handleAuthTokenResponse(messageEvent.data)
            WearableMessagePaths.SESSION_LOGOUT ->
                handleLogout()
            else ->
                android.util.Log.w("WearListener", "unknown path=${messageEvent.path}")
        }
    }

    private fun handleNotification(data: ByteArray, type: NotificationType) {
        runCatching {
            val payload = Json.decodeFromString<WearNotificationPayload>(data.decodeToString())
            eventBus.emitNotification(
                WearNotification(
                    title = payload.title,
                    patientName = payload.patientName,
                    roomLocation = payload.roomLocation,
                    type = type,
                )
            )
        }
    }

    private fun handleAuthTokenResponse(data: ByteArray) {
        val raw = data.decodeToString()
        android.util.Log.d("WearListener", "auth response raw=$raw")
        runCatching {
            val payload = Json.decodeFromString<WearAuthTokenPayload>(raw)
            android.util.Log.d(
                "WearListener",
                "parsed payload wardId=${payload.wardId} tokenBlank=${payload.accessToken.isBlank()}",
            )
            scope.launch {
                tokenStore.save(accessToken = payload.accessToken, wardId = payload.wardId)
                android.util.Log.d("WearListener", "tokenStore.save done wardId=${payload.wardId}")
            }
        }.onFailure {
            android.util.Log.e("WearListener", "handleAuthTokenResponse failed", it)
        }
    }

    private fun handleLogout() {
        scope.launch { tokenStore.clear() }
    }
}
