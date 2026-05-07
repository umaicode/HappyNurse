// NfcReaderEffect — Composable scope 안에서 Activity 의 NFC reader-mode 자동 enable/disable
package com.happynurse.presentation.screens.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun NfcReaderEffect(viewModel: NfcPatientViewModel) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        viewModel.startNfc(activity)
        onDispose { viewModel.stopNfc(activity) }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
