// HappyNurseTheme — Material3 라이트 컬러 스킴(고정) + HnColors 토큰 노출
package com.happynurse.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HnLightColorScheme = lightColorScheme(
    primary = HnColors.Primary,
    onPrimary = HnColors.Surface,
    primaryContainer = HnColors.PrimaryLight,
    onPrimaryContainer = HnColors.PrimaryDark,
    secondary = HnColors.Info,
    onSecondary = HnColors.Surface,
    background = HnColors.Bg,
    onBackground = HnColors.Text,
    surface = HnColors.Surface,
    onSurface = HnColors.Text,
    surfaceTint = HnColors.Surface,
    surfaceVariant = HnColors.SurfaceAlt,
    onSurfaceVariant = HnColors.TextSecondary,
    surfaceContainerLowest = HnColors.Surface,
    surfaceContainerLow = HnColors.Surface,
    surfaceContainer = HnColors.Surface,
    surfaceContainerHigh = HnColors.Surface,
    surfaceContainerHighest = HnColors.SurfaceAlt,
    error = HnColors.Danger,
    onError = HnColors.Surface,
    outline = HnColors.Border,
    outlineVariant = HnColors.BorderStrong,
)

@Composable
fun HappyNurseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HnLightColorScheme,
        typography = HnTypography,
        shapes = HnMaterialShapes,
        content = content,
    )
}
