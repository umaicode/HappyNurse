// NFC reader-mode 라이프사이클 — Composable 진입 시 enable / 이탈 시 disable 자동 처리
package com.happynurse.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

// ViewModel 의 startNfc/stopNfc 콜백을 Activity 라이프사이클에 자동 바인딩.
// activity 가 null 이면(에뮬레이터/Compose preview 등) 아무 것도 안 함.
@Composable
fun NfcLifecycleEffect(
    onStart: (Activity) -> Unit,
    onStop: (Activity) -> Unit,
) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        onStart(activity)
        onDispose { onStop(activity) }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
