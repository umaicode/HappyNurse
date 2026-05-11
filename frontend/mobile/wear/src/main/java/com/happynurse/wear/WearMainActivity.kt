// 워치 단일 Activity — SwipeDismissable NavController 로 WearNavGraph 호스팅.
// GestureService 가 보낸 EXTRA_OPEN_RECORD 가 있으면 녹음 페이지로 자동 진입.
package com.happynurse.wear

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.happynurse.wear.gesture.GestureService
import com.happynurse.wear.presentation.navigation.WearNavGraph
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val autoRecord = consumeAutoRecordExtra(intent)
        setContent {
            HappyNurseWearTheme {
                val navController = rememberSwipeDismissableNavController()
                var autoStartRecord by remember { mutableStateOf(autoRecord) }
                WearNavGraph(
                    navController = navController,
                    autoStartRecord = autoStartRecord,
                    onAutoStartConsumed = { autoStartRecord = false },
                )
            }
        }
    }

    /**
     * 액티비티가 이미 떠 있는 상태에서 풀스크린 인텐트 새로 받은 경우.
     * intent 를 그대로 setIntent 로 등록한 뒤 recreate — 새 onCreate 에서 consumeAutoRecordExtra 가 소비한다.
     * (여기서 미리 consume 하면 recreate 시점에 extra 가 사라져 page 0 로 열림)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(GestureService.EXTRA_OPEN_RECORD, false)) {
            recreate()
        }
    }

    /** EXTRA_OPEN_RECORD 한 번 소비 + intent 정리 + 트리거 알림 dismiss. */
    private fun consumeAutoRecordExtra(intent: Intent?): Boolean {
        val auto = intent?.getBooleanExtra(GestureService.EXTRA_OPEN_RECORD, false) ?: false
        if (auto) {
            intent?.removeExtra(GestureService.EXTRA_OPEN_RECORD)
            runCatching { getSystemService<NotificationManager>()?.cancelAll() }
        }
        return auto
    }
}
