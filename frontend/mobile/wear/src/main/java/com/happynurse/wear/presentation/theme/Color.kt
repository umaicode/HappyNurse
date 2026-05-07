// HappyNurse Wear 컬러 팔레트 — IV/STT/Alarm 등 도메인 색과 Wear M3 ColorScheme 정의.
// ColorScheme() 의 기본 다크 톤 위에 .copy() 로 일부 슬롯만 override 한다.
package com.happynurse.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

// ── HappyNurse 브랜드/도메인 컬러 ──────────────────────────────────────────────
val HnIvBlue = Color(0xFF4DA3FF)
val HnIvBlueDeep = Color(0xFF1E73C7)
val HnSttPurple = Color(0xFFB388FF)
val HnSttOrange = Color(0xFFFFB74D)
val HnAlarmPink = Color(0xFFFF80AB)
val HnUrgent = Color(0xFFFF5252)
val HnSuccess = Color(0xFF4CAF50)
val HnPatientBg = Color(0xFFE91E63)

val HnSurfaceDim = Color(0xFF0E0F12)
val HnSurface = Color(0xFF161A1F)
val HnSurfaceBright = Color(0xFF20242B)
val HnOnSurface = Color(0xFFE6E8EC)
val HnOnSurfaceVariant = Color(0xFFB6BBC4)
val HnOutline = Color(0xFF3A3F47)

internal fun happyNurseColorScheme(): ColorScheme = ColorScheme().copy(
    primary = HnIvBlue,
    onPrimary = Color(0xFF002347),
    primaryContainer = Color(0xFF0D3A66),
    onPrimaryContainer = Color(0xFFD2E5FF),
    secondary = HnSttPurple,
    onSecondary = Color(0xFF22113F),
    secondaryContainer = Color(0xFF3D2C66),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = HnAlarmPink,
    onTertiary = Color(0xFF40081C),
    tertiaryContainer = Color(0xFF5C1B33),
    onTertiaryContainer = Color(0xFFFFD9E4),
    onSurface = HnOnSurface,
    onSurfaceVariant = HnOnSurfaceVariant,
    outline = HnOutline,
    background = HnSurfaceDim,
    onBackground = HnOnSurface,
    error = HnUrgent,
    onError = Color(0xFF410001),
)
