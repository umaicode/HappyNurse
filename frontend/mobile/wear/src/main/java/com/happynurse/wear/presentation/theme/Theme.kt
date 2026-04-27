// HappyNurseWearTheme — Wear Compose MaterialTheme 래퍼
package com.happynurse.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun HappyNurseWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
