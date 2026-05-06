// 와이어프레임 Card 와이트 백그라운드 + rCard(12) + 옅은 그림자 — 클릭 가능 옵션 포함
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.happynurse.presentation.theme.HnColors

@Composable
fun HnCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val base = modifier
        .shadow(elevation = 1.dp, shape = shape)
        .clip(shape)
        .background(HnColors.Surface)
    Surface(
        color = HnColors.Surface,
        shape = shape,
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            content()
        }
    }
}
