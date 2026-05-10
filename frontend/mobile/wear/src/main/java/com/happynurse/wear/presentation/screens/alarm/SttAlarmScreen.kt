// SttAlarmScreen (s13) — STT 타이머 풀스크린 알람 본문. EdgeButton "완료" 시 onDismiss 호출.
package com.happynurse.wear.presentation.screens.alarm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.presentation.components.HnFullScreenAlarmScaffold
import com.happynurse.wear.presentation.theme.HnAccent

@Composable
fun SttAlarmScreen(
    patientName: String,
    contentSummary: String,
    roomBedTime: String,
    onDismiss: () -> Unit,
) {
    HnFullScreenAlarmScaffold(
        badgeText = "ALARM",
        badgeColor = HnAccent,
        patientName = patientName,
        content = contentSummary,
        roomBedTime = roomBedTime,
        bottomButton = {
            EdgeButton(
                onClick = onDismiss,
                modifier = Modifier,
            ) {
                Text("완료", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary)
            }
        },
    )
}
