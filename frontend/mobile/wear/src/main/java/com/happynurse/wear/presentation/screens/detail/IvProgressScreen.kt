// IvProgressScreen — 수액 진행 상세 화면. 베젤 안쪽 큰 원형 progress + 중앙 환자/약물/잔여 시간/종료 시간.
// 뒤로가기는 시스템 스와이프-에지 제스처로 처리한다.
package com.happynurse.wear.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.presentation.theme.remainingTimeColor

@Composable
fun IvProgressScreen(
    iv: IvInfusionTimer,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
) {
    val accentColor = remainingTimeColor(iv.remainingSec)
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 풀스크린 원형 progress — 잔여 시간 임계값에 따라 그린/옐로우/레드
        CircularProgressIndicator(
            progress = { iv.progress },
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = accentColor,
                trackColor = MaterialTheme.colorScheme.outline,
            ),
        )
        // 중앙 정보 스택
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = iv.patientName.ifBlank { "환자" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (iv.patientRoomBed.isNotBlank() && iv.patientRoomBed != "-") {
                Text(
                    text = iv.patientRoomBed,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = iv.medicationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = iv.remainingTimeText.replace(" 남음", ""),
                style = MaterialTheme.typography.displaySmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "종료 시간",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = iv.endAtDisplay,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
