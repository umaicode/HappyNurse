// PhoneDataListenerService — 워치 → 폰 Wearable Data Layer 메시지 수신.
// AUDIO_TIMER (STT raw 오디오) / WEAR_FCM_TOKEN (워치 FCM 토큰 대행 등록) / WEAR_STT_TIMER_CREATE 처리.
package com.happynurse.data.wearable

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneDataListenerService : WearableListenerService() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "워치 → 폰 수신: path=${messageEvent.path} bytes=${messageEvent.data.size}")
        when (messageEvent.path) {
            WearableMessagePaths.AUDIO_TIMER -> handleAudioTimer(messageEvent.data)
            WearableMessagePaths.WEAR_FCM_TOKEN -> handleWearFcmToken(messageEvent.data)
            WearableMessagePaths.WEAR_STT_TIMER_CREATE -> handleWearSttCreate(messageEvent.data)
        }
    }

    // 워치에서 받은 raw audio → 서버 STT API 업로드 → 응답으로 받은 시간(millis) 을
    // WearableMessagePaths.TIMER_START 로 워치에 회신
    private fun handleAudioTimer(data: ByteArray) {
        // TODO 백엔드 STT 엔드포인트 업로드 + millis 응답 수신 + WearDataClient.send(TIMER_START, millisBytes)
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

    /**
     * 워치에서 등록한 STT 타이머를 백엔드 timer/reminder API 에 등록.
     * TODO: 백엔드 STT/Reminder endpoint 합의 후 실제 API 호출 구현.
     */
    private fun handleWearSttCreate(data: ByteArray) {
        val payload = runCatching { String(data, Charsets.UTF_8) }.getOrNull()
        Log.d(TAG, "워치 STT 등록 요청 수신: $payload")
        // TODO: kotlinx.serialization 으로 WearSttCreatePayload 디코드 후
        //       ReminderApi.createReminder(...) 호출 + 등록 결과를 워치에 회신
    }

    companion object {
        private const val TAG = "PhoneDataListener"
    }
}
