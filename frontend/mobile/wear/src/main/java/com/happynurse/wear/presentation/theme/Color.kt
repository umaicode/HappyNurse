// HappyNurse Wear 컬러 팔레트 — 모노톤(거의 검정→짙은 회색) + 인디고 단일 강조색.
// 진단/위험 신호인 잔여시간 색은 RemainingTimeColor 에서 따로 관리.
package com.happynurse.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

// ── 단일 강조색(Indigo) ─────────────────────────────────────────────
val HnAccent = Color(0xFF7C8CFF)
val HnAccentDeep = Color(0xFF4F5FCC)
val HnAccentSoft = Color(0xFF2A3068)
private val HnOnAccent = Color(0xFF0B0C12)
private val HnOnAccentSoft = Color(0xFFE6E9FF)

// ── 모노톤 표면 단계 ────────────────────────────────────────────────
private val MonoBackground = Color(0xFF0A0A0B)
private val MonoSurface = Color(0xFF131316)
private val MonoSurfaceContainerLow = Color(0xFF18181B)
private val MonoSurfaceContainer = Color(0xFF1F1F24)
private val MonoSurfaceContainerHigh = Color(0xFF26262C)
private val MonoSurfaceContainerHighest = Color(0xFF2D2D34)
private val MonoOutline = Color(0xFF3F3F46)

// ── 텍스트 ─────────────────────────────────────────────────────────
private val MonoOnSurface = Color(0xFFE8E8EA)
private val MonoOnSurfaceVariant = Color(0xFF9C9CA3)

// ── 위험 신호(에러/긴급) ─────────────────────────────────────────────
private val HnDanger = Color(0xFFEF4444)
private val HnDangerContainer = Color(0xFF5A1A1A)

internal fun happyNurseColorScheme(): ColorScheme = ColorScheme().copy(
    primary = HnAccent,
    onPrimary = HnOnAccent,
    primaryContainer = HnAccentSoft,
    onPrimaryContainer = HnOnAccentSoft,
    secondary = HnAccent,
    onSecondary = HnOnAccent,
    secondaryContainer = HnAccentSoft,
    onSecondaryContainer = HnOnAccentSoft,
    tertiary = HnAccent,
    onTertiary = HnOnAccent,
    tertiaryContainer = HnAccentSoft,
    onTertiaryContainer = HnOnAccentSoft,
    background = MonoBackground,
    onBackground = MonoOnSurface,
    onSurface = MonoOnSurface,
    onSurfaceVariant = MonoOnSurfaceVariant,
    surfaceContainerLow = MonoSurfaceContainerLow,
    surfaceContainer = MonoSurfaceContainer,
    surfaceContainerHigh = MonoSurfaceContainerHigh,
    outline = MonoOutline,
    error = HnDanger,
    onError = Color(0xFF410001),
    errorContainer = HnDangerContainer,
    onErrorContainer = Color(0xFFFFDAD4),
)
