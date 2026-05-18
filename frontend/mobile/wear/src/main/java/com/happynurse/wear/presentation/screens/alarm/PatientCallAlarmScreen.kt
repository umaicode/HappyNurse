// PatientCallAlarmScreen — 환자 요청 HIGH/CRITICAL 풀스크린 알람.
// 빨간 배경에 경고 아이콘 + 긴급도 라벨 + 환자명/내용/호실 + 확인 EdgeButton.
package com.happynurse.wear.presentation.screens.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

private val EmergencyRed = Color(0xFFD32F2F)
private val OnEmergencyRed = Color.White

@Composable
fun PatientCallAlarmScreen(
    patientName: String,
    body: String,
    roomLocation: String,
    priority: String,
    onDismiss: () -> Unit,
) {
    val priorityLabel = when (priority) {
        "CRITICAL" -> "위급"
        "HIGH" -> "긴급"
        else -> priority
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmergencyRed),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = OnEmergencyRed,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = priorityLabel,
                color = OnEmergencyRed,
                fontWeight = FontWeight.Black,
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                style = androidx.wear.compose.material3.MaterialTheme.typography.labelMedium,
            )
            if (patientName.isNotBlank()) {
                Text(
                    text = patientName,
                    color = OnEmergencyRed,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = androidx.wear.compose.material3.MaterialTheme.typography.displaySmall,
                )
            }
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    color = OnEmergencyRed,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = androidx.wear.compose.material3.MaterialTheme.typography.titleMedium,
                )
            }
            if (roomLocation.isNotBlank()) {
                Text(
                    text = roomLocation,
                    color = OnEmergencyRed.copy(alpha = 0.85f),
                    style = androidx.wear.compose.material3.MaterialTheme.typography.labelSmall,
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
