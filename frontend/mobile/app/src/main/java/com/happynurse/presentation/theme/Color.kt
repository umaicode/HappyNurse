// 와이어프레임 tokens.jsx 의 색 팔레트 1:1 이식 — 의료 앱 일관성을 위해 다이나믹 컬러 미사용
package com.happynurse.presentation.theme

import androidx.compose.ui.graphics.Color
import com.happynurse.domain.model.NotificationCategory
import com.happynurse.domain.model.OrderKind

// 태그 색상 한 쌍 (배경, 글자) — OrderKind / NotificationCategory 매핑에 사용
data class TagColors(val bg: Color, val fg: Color)

object HnColors {
    // Primary
    val Primary       = Color(0xFF1428A0)
    val PrimaryDark   = Color(0xFF0F1F7A)
    val PrimaryLight  = Color(0xFFE6E9F8)
    val PrimarySoft   = Color(0xFFFBFBFF)

    // Surface / background
    val Bg            = Color(0xFFF7F8FA)
    val Surface       = Color(0xFFFFFFFF)
    val SurfaceAlt    = Color(0xFFF8FAFC)
    val Border        = Color(0xFFE5E7EB)
    val BorderStrong  = Color(0xFFD1D5DB)

    // Text
    val Text          = Color(0xFF181818)
    val TextSecondary = Color(0xFF4E4F54)
    val TextTertiary  = Color(0xFF686970)

    // Status
    val Success       = Color(0xFF519470)
    val Warning       = Color(0xFFF59E0B)
    val Danger        = Color(0xFFE76565)
    val Info          = Color(0xFF3B82F6)
    val Purple        = Color(0xFF8B5CF6)
    val Cyan          = Color(0xFF06B6D4)

    // Tag palettes (bg, fg)
    val TagInjBg     = Color(0xFFEEF6F3); val TagInjFg     = Color(0xFF12543C) // 투약 — Primary 계열
    val TagFluidBg   = Color(0xFFEFEFFA); val TagFluidFg   = Color(0xFF152665) // 수액 — 블루 계열
    val TagOrderBg   = Color(0xFFF3F4F6); val TagOrderFg   = TextSecondary     // 지시 — 뉴트럴
    val TagLisBg     = Color(0xFFFFFAEB); val TagLisFg     = Color(0xFF85410C) // LIS  — 앰버
    val TagImgBg     = Color(0xFFE0F7FA); val TagImgFg     = Color(0xFF00838F) // 영상 — 틸
    val TagPillBg    = Color(0xFFF5FAF7); val TagPillFg    = Color(0xFF2E7D32) // 알약 — 그린

    // 알림 시트 전용 — 진한 톤 카테고리 태그
    val TagFluidStrongBg   = Color(0xFFF0F2FE); val TagFluidStrongFg   = Color(0xFF1428A0) // 수액 — Primary 블루
    val TagOrderStrongBg   = Color(0xFFF8F5FE); val TagOrderStrongFg   = Color(0xFF5B21B6) // 의사오더 — 진한 퍼플
    val TagRequestStrongBg = Color(0xFFFFF5F5); val TagRequestStrongFg = Color(0xFFB91C1C) // 환자요청 — 진한 레드
    val TagWatchStrongBg   = Color(0xFFFEFAED); val TagWatchStrongFg   = Color(0xFFB45309) // 워치 — 진한 옐로/앰버

    // OrderKind → 옅은 톤 태그 색상 (환자 상세/오더 리스트용)
    val orderTagColors: Map<OrderKind, TagColors> = mapOf(
        OrderKind.INJ   to TagColors(TagInjBg,   TagInjFg),
        OrderKind.FLUID to TagColors(TagFluidBg, TagFluidFg),
        OrderKind.ORDER to TagColors(TagOrderBg, TagOrderFg),
        OrderKind.LIS   to TagColors(TagLisBg,   TagLisFg),
        OrderKind.IMG   to TagColors(TagImgBg,   TagImgFg),
        OrderKind.PILL  to TagColors(TagPillBg,  TagPillFg),
    )

    // NotificationCategory → 진한 톤 태그 색상 (알림 시트용)
    val notificationTagColors: Map<NotificationCategory, TagColors> = mapOf(
        NotificationCategory.FLUID   to TagColors(TagFluidStrongBg,   TagFluidStrongFg),
        NotificationCategory.ORDER   to TagColors(TagOrderStrongBg,   TagOrderStrongFg),
        NotificationCategory.REQUEST to TagColors(TagRequestStrongBg, TagRequestStrongFg),
        NotificationCategory.WATCH   to TagColors(TagWatchStrongBg,   TagWatchStrongFg),
    )
}
