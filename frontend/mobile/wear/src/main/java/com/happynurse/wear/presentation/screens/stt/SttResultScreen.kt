// SttResultScreen (s12) — STT 변환 결과 확인 화면. 인식된 시간/원문/하이라이트와 "확인" EdgeButton.
package com.happynurse.wear.presentation.screens.stt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.presentation.components.HnRoomBedPill
import com.happynurse.wear.presentation.theme.HnSttPurple
import com.happynurse.wear.presentation.theme.HnSuccess
import com.happynurse.wear.presentation.theme.TabularNumStyle

@Composable
fun SttResultScreen(
    patientName: String,
    roomBed: String,
    timeDisplay: String,
    sttText: String,
    highlightStart: Int,
    highlightEnd: Int,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 18.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = patientName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            HnRoomBedPill(text = roomBed)
            TimeAckPill()
            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayMedium.merge(TabularNumStyle),
                color = HnSttPurple,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = highlightedText(sttText, highlightStart, highlightEnd),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        EdgeButton(
            onClick = onConfirm,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Text(text = "확인", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeAckPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HnSuccess.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = HnSuccess,
                modifier = Modifier.size(12.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
            Text(
                text = "시간 인식 완료",
                style = MaterialTheme.typography.labelSmall,
                color = HnSuccess,
            )
        }
    }
}

private fun highlightedText(text: String, start: Int, end: Int): AnnotatedString {
    if (start < 0 || end <= start || end > text.length) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(color = HnSttPurple, fontWeight = FontWeight.Bold, background = HnSttPurple.copy(alpha = 0.18f))) {
            append(text.substring(start, end))
        }
        append(text.substring(end))
    }
}

