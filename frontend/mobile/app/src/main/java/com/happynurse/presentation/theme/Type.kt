// 타이포그래피 — Pretendard Variable 폰트(wght 축)로 weight 별 슬롯 구성.
package com.happynurse.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.happynurse.R

@OptIn(ExperimentalTextApi::class)
private fun pretendard(weight: FontWeight) = Font(
    resId = R.font.pretendard,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Pretendard: FontFamily = FontFamily(
    pretendard(FontWeight.Thin),
    pretendard(FontWeight.ExtraLight),
    pretendard(FontWeight.Light),
    pretendard(FontWeight.Normal),
    pretendard(FontWeight.Medium),
    pretendard(FontWeight.SemiBold),
    pretendard(FontWeight.Bold),
    pretendard(FontWeight.ExtraBold),
    pretendard(FontWeight.Black),
)

val HnTypography = Typography(
    titleLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold,     fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).sp),
    titleMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold,    fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal,    fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal,    fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 14.sp),
)
