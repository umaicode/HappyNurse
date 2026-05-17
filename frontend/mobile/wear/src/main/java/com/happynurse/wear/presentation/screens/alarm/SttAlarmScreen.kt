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
    contentSummary: String,
    onDismiss: () -> Unit,
) {
    HnFullScreenAlarmScaffold(
        badgeText = "알람",
        badgeColor = HnAccent,
        content = contentSummary,
        bottomButton = {
            EdgeButton(
                onClick = onDismiss,
                modifier = Modifier,
            ) {
                Text(
                    text = "확인",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
    )
}
