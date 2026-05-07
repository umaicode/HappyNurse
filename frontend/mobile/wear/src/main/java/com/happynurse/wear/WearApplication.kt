// 워치 앱 Hilt DI 진입점 — 워치 프로세스 시작 시 의존성 그래프 초기화 + FCM 토큰 폰 forward.
package com.happynurse.wear

import android.app.Application
import com.happynurse.wear.data.fcm.SystemNotifBuilder
import com.happynurse.wear.data.fcm.WearFcmTokenForwarder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject lateinit var tokenForwarder: WearFcmTokenForwarder

    override fun onCreate() {
        super.onCreate()
        SystemNotifBuilder.ensureChannel(this)
        // 앱 시작 시 현재 FCM 토큰을 폰에 forward — 폰이 백엔드(deviceType=watch)에 대행 등록
        tokenForwarder.forwardCurrentToken()
    }
}
