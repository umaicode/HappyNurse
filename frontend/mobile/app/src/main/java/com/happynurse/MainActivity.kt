// 단일 Activity — Compose setContent 로 NavGraph 호스팅. 알림 권한 요청과 FCM 토큰 등록 진입점
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import com.happynurse.presentation.navigation.NavGraph
import com.happynurse.presentation.navigation.NavRoutes
import com.happynurse.presentation.theme.HappyNurseTheme
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 거부해도 앱 정상 동작, 알림만 미표시 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        fcmTokenRegistrar.registerCurrentToken()
        val nfcToken = intent.getStringExtra(NfcEntryActivity.EXTRA_NFC_TOKEN)
        setContent {
            HappyNurseTheme {
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                    LaunchedEffect(nfcToken) {
                        if (nfcToken != null) navController.navigate(NavRoutes.NFC_PATIENT)
                    }
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
