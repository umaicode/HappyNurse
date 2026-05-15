// 워치 앱 Hilt DI 진입점 — 워치 프로세스 시작 시 의존성 그래프 초기화 + FCM 토큰 폰 forward.
package com.happynurse.wear

import android.app.Application
import com.happynurse.wear.alarm.AlarmTtsSpeaker
import com.happynurse.wear.data.remote.fcm.SystemNotificationBuilder
import com.happynurse.wear.data.remote.fcm.WearFcmTokenForwarder
import com.happynurse.wear.gesture.GestureServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject lateinit var tokenForwarder: WearFcmTokenForwarder
    @Inject lateinit var gestureServiceController: GestureServiceController
    @Inject lateinit var alarmTtsSpeaker: AlarmTtsSpeaker

    override fun onCreate() {
        super.onCreate()
        SystemNotificationBuilder.ensureChannel(this)
        // 앱 시작 시 현재 FCM 토큰을 폰에 forward — 폰이 백엔드(deviceType=watch)에 대행 등록
        tokenForwarder.forwardCurrentToken()
        // 로그인 상태이면 손목 제스처 단축 서비스 활성화
        gestureServiceController.bind()
        // 알람 TTS 엔진 미리 워밍업 — 알람 발화 시점에 즉시 speak() 가능하도록
        alarmTtsSpeaker.warmUp()
    }
}
