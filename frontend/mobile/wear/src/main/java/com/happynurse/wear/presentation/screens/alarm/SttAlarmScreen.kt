// SttAlarmScreen (s13) — STT 타이머 풀스크린 알람 본문. EdgeButton "완료" 시 onDismiss 호출.
package com.happynurse.wear.presentation.screens.alarm

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import com.happynurse.wear.presentation.components.HnFullScreenAlarmScaffold

@Composable
fun SttAlarmScreen(
    contentSummary: String,
    onDismiss: () -> Unit,
) {
    HnFullScreenAlarmScaffold(
        content = contentSummary,
        bottomButton = {
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
        },
    )
}