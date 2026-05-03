// 폰 → 워치 메시지 수신 서비스. 수신한 페이로드를 WearEventBus 로 emit 해 ViewModel 이 collect
package com.happynurse.wear.data.remote

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.happynurse.wear.data.notification.NotificationType
import com.happynurse.wear.data.notification.WearEventBus
import com.happynurse.wear.data.notification.WearNotification
import com.happynurse.wear.data.notification.WearNotificationPayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    @Inject lateinit var eventBus: WearEventBus

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            WearableMessagePaths.IV_ALERT ->
                handleNotification(messageEvent.data, NotificationType.IV_ALERT)
            WearableMessagePaths.TIMER_ALARM ->
                handleNotification(messageEvent.data, NotificationType.TIMER_ALARM)
            WearableMessagePaths.PATIENT_CALL ->
                handleNotification(messageEvent.data, NotificationType.PATIENT_CALL)
            WearableMessagePaths.TIMER_START ->
                handleTimerStart(messageEvent.data)
            WearableMessagePaths.SESSION_LOGOUT ->
                handleLogout()
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

    private fun handleTimerStart(data: ByteArray) {
        if (data.size != Long.SIZE_BYTES) return
        val millis = ByteBuffer.wrap(data).long
        eventBus.emitTimerStart(millis)
    }

    private fun handleLogout() {
        // TODO 워치 로컬 세션 파기
    }
}
