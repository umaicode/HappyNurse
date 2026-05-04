package com.happynurse.data.wearable

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneDataListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "워치 → 폰 수신: path=${messageEvent.path} bytes=${messageEvent.data.size}")
        when (messageEvent.path) {
            WearableMessagePaths.AUDIO_TIMER -> handleAudioTimer(messageEvent.data)
        }
    }

    // 워치에서 받은 raw audio → 서버 STT API 업로드 → 응답으로 받은 시간(millis) 을
    // WearableMessagePaths.TIMER_START 로 워치에 회신
    private fun handleAudioTimer(data: ByteArray) {
        // TODO 백엔드 STT 엔드포인트 업로드 + millis 응답 수신 + WearDataClient.send(TIMER_START, millisBytes)
    }

    companion object {
        private const val TAG = "PhoneDataListener"
    }
}
