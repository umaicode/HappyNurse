// HnPulseRing — s11 녹음 중 stop 아이콘 외곽의 펄스 링 애니메이션(1.6초 무한).
package com.happynurse.wear.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun HnPulseRing(
    diameterDp: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseProgress",
    )
    Canvas(modifier = modifier.size(diameterDp.dp)) {
        val radius = (size.minDimension / 2f) * (0.6f + 0.4f * progress)
        val alpha = 1f - progress
        drawCircle(
            color = color.copy(alpha = alpha * 0.5f),
            radius = radius,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}
