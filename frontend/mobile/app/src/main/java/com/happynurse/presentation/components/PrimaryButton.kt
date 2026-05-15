// 와이어프레임 Button(primary/secondary) — 52dp 높이, 풀폭 옵션, 비활성/로딩 처리
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.theme.HnColors

enum class HnButtonVariant { PRIMARY, SECONDARY, DANGER }

@Composable
fun HnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HnButtonVariant = HnButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    full: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    val (bg, fg, borderColor) = when {
        !enabled -> Triple(Color(0xFFD1D5DB), Color.White, null)
        variant == HnButtonVariant.PRIMARY -> Triple(HnColors.Primary, Color.White, null)
        variant == HnButtonVariant.SECONDARY -> Triple(HnColors.Surface, HnColors.Primary, HnColors.Primary)
        else -> Triple(HnColors.Surface, HnColors.Danger, HnColors.Danger)
    }

    val widthMod = if (full) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .then(widthMod)
            .height(58.dp)
            .clip(shape)
            .let { if (borderColor != null) it.border(1.dp, borderColor, shape) else it }
            .background(bg)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Text(" ", color = fg)
            }
            Text(text, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
