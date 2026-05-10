// 인수인계 탭 — 백엔드 API 미구현. 임시 빈 상태 표시.
package com.happynurse.presentation.screens.handoff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoffScreen() {
    Column(Modifier.fillMaxWidth()) {
        PageHeader(title = "인수인계", sub = "데이 → 이브닝")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Inbox,
                contentDescription = null,
                tint = HnColors.TextTertiary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "인계 항목이 없습니다",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = HnColors.TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "준비중인 기능입니다",
                fontSize = 12.sp,
                color = HnColors.TextTertiary,
            )
        }
    }
}
