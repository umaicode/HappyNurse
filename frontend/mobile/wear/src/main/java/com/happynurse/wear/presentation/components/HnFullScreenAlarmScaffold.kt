// HnFullScreenAlarmScaffold — s09(수액 종료) / s13(STT 타이머) 풀스크린 알람의 공통 레이아웃.
// 상단 앱 아이콘 + 배지 + 환자명 + 본문 + 호실/시간 + 하단 EdgeButton "완료" 슬롯을 제공한다.
package com.happynurse.wear.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.R

@Composable
fun HnFullScreenAlarmScaffold(
    badgeText: String,
    badgeColor: Color,
    content: String,
    bottomButton: @Composable () -> Unit,
    patientName: String = "",
    roomBedTime: String = "",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // top: 시계 영역(약 28dp) 바로 아래부터 콘텐츠 시작
                // bottom: 하단 EdgeButton(약 56dp) 와 겹치지 않게 여유
                .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // 모든 요소를 세로 중앙에 배치 — content 가 화면 중앙에 보이도록.
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            // 앱 아이콘 — badgeText 바로 위에 작게.
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
            )
            if (patientName.isNotBlank()) {
                Text(
                    text = patientName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Text(
                text = content,
                // 사용자 요청 — 본문 글자 한 단계씩 키움.
                style = if (patientName.isBlank()) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (patientName.isBlank()) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (roomBedTime.isNotBlank()) {
                Text(
                    text = roomBedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
            bottomButton()
        }
    }
}
