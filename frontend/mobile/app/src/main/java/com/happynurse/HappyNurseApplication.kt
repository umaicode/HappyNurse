// Hilt DI 진입점 — 앱 프로세스 시작 시 의존성 그래프 초기화
package com.happynurse

import android.app.Application
import com.happynurse.data.remote.fcm.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HappyNurseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }
}