// 카드/버튼/칩/시트 라운드 토큰 — tokens.jsx 의 rCard/rBtn/rChip/rSheet 와 동일
package com.happynurse.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object HnShapes {
    val Card  = RoundedCornerShape(12.dp)
    val Btn   = RoundedCornerShape(10.dp)
    val Chip  = RoundedCornerShape(6.dp)
    val Sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

val HnMaterialShapes = Shapes(
    extraSmall = HnShapes.Chip,
    small = HnShapes.Btn,
    medium = HnShapes.Card,
    large = HnShapes.Sheet,
)
