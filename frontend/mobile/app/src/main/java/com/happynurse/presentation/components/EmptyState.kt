// 빈 상태 표시 — 아이콘 + 제목 + 보조설명 + 선택적 액션 버튼
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

// 컴팩트 빈 상태 — 상단 여백 + 작은 회색 아이콘 + 텍스트 (예: 업무 페이지 빈 탭)
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HnColors.TextTertiary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = HnColors.TextTertiary)
    }
}

// CTA 가 있는 빈 상태 — 원형 강조 아이콘 + 제목 + 설명 + 액션 버튼 (예: 담당 환자 선택)
@Composable
fun EmptyStateWithAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(HnColors.PrimarySoft),
        ) {
            Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = HnColors.TextSecondary)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(HnColors.Primary)
                .clickable(onClick = onAction)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(actionLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
