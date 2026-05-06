// 태그 칩 — 와이어프레임 Tag 컴포넌트와 동일한 라운드 칩 (배경/글자색을 직접 받음)
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.theme.HnColors

@Composable
fun TagChip(
    text: String,
    fg: Color = HnColors.TextSecondary,
    bg: Color = HnColors.SurfaceAlt,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
