// 단일 Activity — Compose setContent로 NavGraph를 호스팅
package com.happynurse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import com.happynurse.NfcEntryActivity.Companion.EXTRA_NFC_TOKEN
import com.happynurse.presentation.navigation.NavGraph
import com.happynurse.presentation.ui.theme.HappyNurseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 로그인 화면 도입 후 LoginViewModel 또는 인증 흐름 안으로 이동 예정
    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 결과 무관 — 거부해도 앱 정상 동작, 알림만 미표시 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        fcmTokenRegistrar.fetchAndLog()
        val nfcToken = intent.getStringExtra(EXTRA_NFC_TOKEN)
        setContent {
            HappyNurseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController, nfcToken = nfcToken)
                }
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}