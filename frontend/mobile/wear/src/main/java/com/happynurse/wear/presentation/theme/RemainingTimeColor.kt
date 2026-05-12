// 남은 시간(초)에 따라 그린/옐로우/레드 색을 결정하는 함수.
// 모노톤 배경에 자연스럽게 녹아들도록 채도를 한 단계 낮춘 톤을 사용한다.
package com.happynurse.wear.presentation.theme

import androidx.compose.ui.graphics.Color

val RemainingTimeGreen = Color(0xFF4ADE80)   // 안전 — 부드러운 그린
val RemainingTimeYellow = Color(0xFFE8C547)  // 주의 — 차분한 앰버
val RemainingTimeRed = Color(0xFFF87171)     // 위험 — 가독성 위해 약간 선명한 코랄 레드

fun remainingTimeColor(remainingSec: Int): Color = when {
    remainingSec <= 600 -> RemainingTimeRed       // 10분 이하
    remainingSec <= 1800 -> RemainingTimeYellow   // 30분 이하
    else -> RemainingTimeGreen
}
