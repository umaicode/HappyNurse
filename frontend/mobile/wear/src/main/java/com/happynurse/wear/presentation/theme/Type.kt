// HappyNurse Wear Typography — Roboto Flex variable font 기반의 Wear M3 Typography.
// 워치 가독성을 위해 본문 weight 는 400/500, 강조는 700 위주로 설정.
package com.happynurse.wear.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import com.happynurse.wear.R

val RobotoFlex: FontFamily = FontFamily(
    Font(R.font.roboto_flex, FontWeight.Light),
    Font(R.font.roboto_flex, FontWeight.Normal),
    Font(R.font.roboto_flex, FontWeight.Medium),
    Font(R.font.roboto_flex, FontWeight.Bold),
)

// 카운트다운 / 시간 디스플레이용 — tabular figures(폭 고정 숫자) 활성화
val TabularNumStyle: TextStyle = TextStyle(
    fontFamily = RobotoFlex,
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Bold,
)

internal fun happyNurseTypography(): Typography {
    val base = Typography()
    fun TextStyle.withFlex(weight: FontWeight = this.fontWeight ?: FontWeight.Normal) =
        copy(fontFamily = RobotoFlex, fontWeight = weight)
    return base.copy(
        displayLarge = base.displayLarge.withFlex(FontWeight.Bold),
        displayMedium = base.displayMedium.withFlex(FontWeight.Bold),
        displaySmall = base.displaySmall.withFlex(FontWeight.Medium),
        titleLarge = base.titleLarge.withFlex(FontWeight.Bold),
        titleMedium = base.titleMedium.withFlex(FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFlex(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.withFlex(FontWeight.Normal),
        bodyMedium = base.bodyMedium.withFlex(FontWeight.Normal),
        bodySmall = base.bodySmall.withFlex(FontWeight.Normal),
        labelLarge = base.labelLarge.withFlex(FontWeight.Medium),
        labelMedium = base.labelMedium.withFlex(FontWeight.Medium),
        labelSmall = base.labelSmall.withFlex(FontWeight.Medium),
    )
}
