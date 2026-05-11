// 헤더 우측 종 아이콘 + 미확인 카운트 배지 — 알림 시트 열기에 사용
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.theme.HnColors

@Composable
fun NotifBell(
    unreadCount: Int,
    onClick: () -> Unit,
    bgRing: Color = HnColors.Bg,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
    ) {
        Icon(
            Icons.Outlined.Notifications,
            contentDescription = "알림",
            tint = HnColors.Text,
            modifier = Modifier.size(28.dp),
        )
        if (unreadCount > 0) {
            val display = if (unreadCount > 9) "9+" else unreadCount.toString()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(HnColors.Danger)
            ) {
                Text(
                    text = display,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
