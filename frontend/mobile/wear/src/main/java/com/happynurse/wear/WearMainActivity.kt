// 워치 단일 Activity — SwipeDismissable NavController 로 WearNavGraph 호스팅.
// GestureService 가 보낸 EXTRA_OPEN_RECORD 가 도착하면 gestureRecordRequest 로 신호 → NavGraph 가 HomePager 로 강제 이동 + 자동 녹음.
package com.happynurse.wear

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
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

    // 제스처 → "녹음 화면으로 가서 자동 시작" 요청 신호.
    // onNewIntent 에서 recreate() 를 쓰면 SwipeDismissableNavController 가 savedInstanceState 로
    // 이전 destination 을 복원해 마지막 화면이 그대로 남는 버그가 있어, StateFlow 로 NavGraph 에 직접 신호.
    private val gestureRecordRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        if (consumeAutoRecordExtra(intent)) {
            gestureRecordRequest.value = true
        }
        setContent {
            HappyNurseWearTheme {
                val navController = rememberSwipeDismissableNavController()
                val autoRecord by gestureRecordRequest.collectAsStateWithLifecycle()
                WearNavGraph(
                    navController = navController,
                    autoStartRecord = autoRecord,
                    onAutoStartConsumed = { gestureRecordRequest.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (consumeAutoRecordExtra(intent)) {
            gestureRecordRequest.value = true
        }
    }

    /**
     * EXTRA_OPEN_RECORD 한 번 소비 + intent 정리 + 트리거 알림 dismiss.
     * EXTRA_FIRED_AT 타임스탬프가 GESTURE_INTENT_TTL_MS 보다 오래된 경우 무시 — 좀비 인텐트 방지.
     */
    private fun consumeAutoRecordExtra(intent: Intent?): Boolean {
        val auto = intent?.getBooleanExtra(GestureService.EXTRA_OPEN_RECORD, false) ?: false
        if (!auto) return false
        val firedAt = intent.getLongExtra(GestureService.EXTRA_FIRED_AT, 0L)
        val age = System.currentTimeMillis() - firedAt
        intent.removeExtra(GestureService.EXTRA_OPEN_RECORD)
        intent.removeExtra(GestureService.EXTRA_FIRED_AT)
        if (firedAt <= 0L || age > GestureService.GESTURE_INTENT_TTL_MS) {
            // 너무 오래된 인텐트 — 사용자가 한참 뒤 앱 manually 진입한 케이스. 자동 녹음 X.
            return false
        }
        runCatching { getSystemService<NotificationManager>()?.cancelAll() }
        return true
    }
}
