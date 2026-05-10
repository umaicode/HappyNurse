// 와이어프레임 tokens.jsx 의 색 팔레트 1:1 이식 — 의료 앱 일관성을 위해 다이나믹 컬러 미사용
package com.happynurse.presentation.theme

import androidx.compose.ui.graphics.Color

object HnColors {
    // Primary
    val Primary       = Color(0xFF1428A0)
    val PrimaryDark   = Color(0xFF0F1F7A)
    val PrimaryLight  = Color(0xFFE6E9F8)
    val PrimarySoft   = Color(0xFFF0F4FF)

    // Surface / background
    val Bg            = Color(0xFFF7F8FA)
    val Surface       = Color(0xFFFFFFFF)
    val SurfaceAlt    = Color(0xFFF8FAFC)
    val Border        = Color(0xFFE5E7EB)
    val BorderStrong  = Color(0xFFD1D5DB)

    // Text
    val Text          = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF464B50)
    val TextTertiary  = Color(0xFF9CA3AF)

    // Status
    val Success       = Color(0xFF0B6C4C)
    val Warning       = Color(0xFFF59E0B)
    val Danger        = Color(0xFFEF4444)
    val Info          = Color(0xFF3B82F6)
    val Purple        = Color(0xFF8B5CF6)
    val Cyan          = Color(0xFF06B6D4)

    // Tag palettes (bg, fg)
    val TagInjBg     = Color(0xFFEEF6F3); val TagInjFg     = Color(0xFF12543C) // 투약 — Primary 계열
    val TagFluidBg   = Color(0xFFEFEFFA); val TagFluidFg   = Color(0xFF152665) // 수액 — 블루 계열
    val TagOrderBg   = Color(0xFFF3F4F6); val TagOrderFg   = TextSecondary     // 지시 — 뉴트럴
    val TagLisBg     = Color(0xFFFFF8E1); val TagLisFg     = Color(0xFFB45309) // LIS  — 앰버
    val TagImgBg     = Color(0xFFE0F7FA); val TagImgFg     = Color(0xFF00838F) // 영상 — 틸
    val TagPillBg    = Color(0xFFE8F5E9); val TagPillFg    = Color(0xFF2E7D32) // 알약 — 그린
}
