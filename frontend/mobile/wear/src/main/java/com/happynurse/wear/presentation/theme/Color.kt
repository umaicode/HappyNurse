// HappyNurse Wear 컬러 팔레트 — Material 3 Expressive 톤(채도 ↑, surface 단계 분리).
package com.happynurse.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

// ── HappyNurse 브랜드/도메인 컬러 ──────────────────────────────────────────────
val HnIvBlue = Color(0xFF5BB0FF)
val HnIvBlueDeep = Color(0xFF1E73C7)
val HnSttPurple = Color(0xFFC4A7FF)
val HnSttOrange = Color(0xFFFFB74D)
val HnAlarmPink = Color(0xFFFF8FB8)
val HnUrgent = Color(0xFFFF6B6B)
val HnSuccess = Color(0xFF4CAF50)
val HnPatientBg = Color(0xFFE91E63)

// Surface 단계(Expressive: Container/High/Highest 분리)
val HnSurfaceDim = Color(0xFF0B0C10)
val HnSurface = Color(0xFF14171D)
val HnSurfaceContainer = Color(0xFF1B1F26)
val HnSurfaceContainerHigh = Color(0xFF242932)
val HnSurfaceContainerHighest = Color(0xFF2D333E)
val HnSurfaceBright = Color(0xFF353C48)
val HnOnSurface = Color(0xFFEEF0F4)
val HnOnSurfaceVariant = Color(0xFFB6BBC4)
val HnOutline = Color(0xFF3A3F47)

internal fun happyNurseColorScheme(): ColorScheme = ColorScheme().copy(
    primary = HnIvBlue,
    onPrimary = Color(0xFF002347),
    primaryContainer = Color(0xFF124B85),
    onPrimaryContainer = Color(0xFFD9EBFF),
    secondary = HnSttPurple,
    onSecondary = Color(0xFF22113F),
    secondaryContainer = Color(0xFF4A357A),
    onSecondaryContainer = Color(0xFFEFE5FF),
    tertiary = HnAlarmPink,
    onTertiary = Color(0xFF40081C),
    tertiaryContainer = Color(0xFF6A2240),
    onTertiaryContainer = Color(0xFFFFDCE7),
    onSurface = HnOnSurface,
    onSurfaceVariant = HnOnSurfaceVariant,
    surfaceContainer = HnSurfaceContainer,
    surfaceContainerHigh = HnSurfaceContainerHigh,
    outline = HnOutline,
    background = HnSurfaceDim,
    onBackground = HnOnSurface,
    error = HnUrgent,
    onError = Color(0xFF410001),
    errorContainer = Color(0xFF7A1D1D),
    onErrorContainer = Color(0xFFFFDAD4),
)
