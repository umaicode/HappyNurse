// HappyNurseWearTheme — Wear Compose Material 3 MaterialTheme 래퍼.
// ColorScheme + Typography 를 주입한다.
package com.happynurse.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun HappyNurseWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = happyNurseColorScheme(),
        typography = happyNurseTypography(),
        content = content,
    )
}
