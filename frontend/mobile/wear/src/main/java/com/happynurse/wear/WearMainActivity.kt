// 워치 단일 Activity — SwipeDismissable NavController 로 WearNavGraph 호스팅.
// GestureService 가 보낸 EXTRA_OPEN_RECORD 가 도착하면 gestureRecordRequest 로 신호 → NavGraph 가 HomePager 로 강제 이동 + 자동 녹음.
package com.happynurse.wear

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.happynurse.wear.gesture.GestureService
import com.happynurse.wear.presentation.navigation.WearNavGraph
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    // 제스처 → "녹음 화면으로 가서 자동 시작" 요청 신호. 0L=신호 없음, > 0=trigger timestamp.
    // Boolean StateFlow 였을 때 같은 값(true) 반복 emit 이 무시되어 백그라운드에서 재진입 시
    // LaunchedEffect 가 재실행 안 되는 버그가 있었다. timestamp 로 매번 unique 한 값을 emit 한다.
    private val gestureRecordRequest = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        consumeAutoRecordExtra(intent)?.let { ts ->
            gestureRecordRequest.value = ts
            // 제스처 진입 직후 ~수백ms 동안 RecordScreen DisposableEffect 가 아직 안 붙은 공백 구간에
            // Wear OS inactivity timer 가 화면을 끄는 race 차단. 이후 RecordScreen 의 phase 기반 제어가 인수.
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContent {
            HappyNurseWearTheme {
                val navController = rememberSwipeDismissableNavController()
                val autoRecordTrigger by gestureRecordRequest.collectAsStateWithLifecycle()
                WearNavGraph(
                    navController = navController,
                    autoStartRecordTrigger = autoRecordTrigger,
                    onAutoStartConsumed = { gestureRecordRequest.value = 0L },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAutoRecordExtra(intent)?.let { ts ->
            gestureRecordRequest.value = ts
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * EXTRA_OPEN_RECORD 한 번 소비 + intent 정리 + 트리거 알림 dismiss.
     * 반환값: 유효한 trigger 면 firedAt timestamp, 아니면 null. 매번 unique 한 timestamp 가 들어와
     * StateFlow distinctUntilChanged 를 통과해 LaunchedEffect 가 항상 재실행되도록 한다.
     */
    private fun consumeAutoRecordExtra(intent: Intent?): Long? {
        val auto = intent?.getBooleanExtra(GestureService.EXTRA_OPEN_RECORD, false) ?: false
        if (!auto) return null
        val firedAt = intent.getLongExtra(GestureService.EXTRA_FIRED_AT, 0L)
        val age = System.currentTimeMillis() - firedAt
        intent.removeExtra(GestureService.EXTRA_OPEN_RECORD)
        intent.removeExtra(GestureService.EXTRA_FIRED_AT)
        if (firedAt <= 0L || age > GestureService.GESTURE_INTENT_TTL_MS) {
            // 너무 오래된 인텐트 — 사용자가 한참 뒤 앱 manually 진입한 케이스. 자동 녹음 X.
            return null
        }
        runCatching { getSystemService<NotificationManager>()?.cancelAll() }
        return firedAt
    }
}
