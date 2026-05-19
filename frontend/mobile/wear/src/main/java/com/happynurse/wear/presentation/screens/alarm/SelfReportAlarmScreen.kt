// SelfReportAlarmScreen — 환자요청 위급/높음 풀스크린 알람.
// InfiniteTransition 으로 빨간 배경 알파를 0.4↔1.0 반복(450ms) — 깜빡거리는 경고 효과.
package com.happynurse.wear.presentation.screens.alarm

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun SelfReportAlarmScreen(
    patientName: String,
    roomLocation: String,
    body: String,
    priority: String,
    onDismiss: () -> Unit,
) {
    val isCritical = priority.equals("CRITICAL", ignoreCase = true)
    val baseColor = if (isCritical) Color(0xFFD32F2F) else Color(0xFFE1685F)

    val infinite = rememberInfiniteTransition(label = "self-report-flash")
    val alpha by infinite.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseColor.copy(alpha = alpha)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = if (isCritical) "위급" else "높음",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            if (patientName.isNotBlank()) {
                Text(
                    text = patientName,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Text(
                text = body,
                style = if (patientName.isBlank()) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = Color.White,
                fontWeight = if (patientName.isBlank()) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (roomLocation.isNotBlank()) {
                Text(
                    text = roomLocation,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.size(0.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            EdgeButton(
                onClick = onDismiss,
                modifier = Modifier,
                buttonSize = EdgeButtonSize.Small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "확인",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
