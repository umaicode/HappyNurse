// 단일 Activity — Compose setContent 로 NavGraph 호스팅. 알림 권한 요청과 FCM 토큰 등록 진입점
// onNewIntent override 로 NfcEntryActivity → MainActivity (이미 foreground) 흐름의 token 묵살을 방지
package com.happynurse

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import com.happynurse.presentation.navigation.NavGraph
import com.happynurse.presentation.navigation.NavRoutes
import com.happynurse.presentation.theme.HappyNurseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    // onCreate 와 onNewIntent 모두에서 갱신해 Compose 가 token 을 한 곳에서 관찰
    private val nfcTokenState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 거부해도 앱 정상 동작, 알림만 미표시 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        fcmTokenRegistrar.registerCurrentToken()
        nfcTokenState.value = intent.getStringExtra(NfcEntryActivity.EXTRA_NFC_TOKEN)
        setContent {
            HappyNurseTheme {
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                    val token by nfcTokenState
                    LaunchedEffect(token) {
                        val current = token ?: return@LaunchedEffect
                        navController.navigate(NavRoutes.nfcPatient(current))
                        nfcTokenState.value = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcTokenState.value = intent.getStringExtra(NfcEntryActivity.EXTRA_NFC_TOKEN)
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
