// Hilt DI 진입점 — 앱 프로세스 시작 시 의존성 그래프 초기화
package com.happynurse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HappyNurseApplication : Application()