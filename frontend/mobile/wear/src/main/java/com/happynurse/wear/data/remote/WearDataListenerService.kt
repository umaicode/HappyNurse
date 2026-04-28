// 폰→워치 메시지 수신 서비스 — /notification, /session 경로의 DataLayer 메시지 처리
package com.happynurse.wear.data.remote

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint

// 폰에서 워치로 오는 메시지 수신 (알림_043 수액 알림, 알림_044 타이머 알람)
@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            "/notification/iv_alert" -> handleIvAlert(messageEvent.data)
            "/notification/timer_alarm" -> handleTimerAlarm(messageEvent.data)
            "/session/logout" -> handleLogout()
        }
    }

    // 알림_043: 수액 알림 처리
    private fun handleIvAlert(data: ByteArray) {
        // TODO: 진동 + 화면 표시
    }

    // 알림_044: 타이머 알람 처리
    private fun handleTimerAlarm(data: ByteArray) {
        // TODO: 진동 + 화면 표시
    }

    // 회원_006: 폰 로그아웃 시 워치 세션 동시 해제
    private fun handleLogout() {
        // TODO: 워치 로컬 세션 파기
    }
}
