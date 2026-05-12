// 알림 바텀시트 — 카테고리별 색칩 + 긴급(빨간 점) + 시간 라벨
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.Notification
import com.happynurse.domain.model.NotificationCategory
import com.happynurse.presentation.theme.HnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    visible: Boolean,
    notifications: List<Notification>,
    onClose: () -> Unit,
    onDelete: (String) -> Unit = {},
    onDeleteAll: () -> Unit = {},
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = HnColors.Surface,
    ) {
        // 과거 24h ~ 미래 24h 알림만 표시 (워치알람은 minutesAgo 가 음수)
        val recent = notifications.filter { it.minutesAgo in -1440..1440 }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
        ) {
            Text("알림", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text, modifier = Modifier.weight(1f))
            if (recent.isNotEmpty()) {
                Text(
                    "모두 지우기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDeleteAll)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = HnColors.TextSecondary)
            }
        }
        if (recent.isEmpty()) {
            // 담당 환자가 없거나 그 환자에 대한 알림이 없을 때
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
            ) {
                Text(
                    "담당 환자의 알림이 없습니다",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.Text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "환자 탭에서 담당환자를 선택하면\n해당 환자의 알림이 여기 표시됩니다",
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                )
                Spacer(Modifier.height(20.dp))
            }
        } else {
            val sorted = recent.sortedWith(
                compareBy<Notification> { it.minutesAgo },
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                items(sorted, key = { it.id }) { notification ->
                    SwipeToDeleteRow(onDelete = { onDelete(notification.id) }) {
                        NotificationRow(notification)
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart },
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val progress = dismissState.progress.coerceIn(0f, 1f)
            val direction = dismissState.dismissDirection
            val active = direction == SwipeToDismissBoxValue.EndToStart && progress > 0.02f
            val committed = progress > 0.4f
            val bgAlpha = if (active) (0.15f + 0.85f * progress).coerceAtMost(1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HnColors.Danger.copy(alpha = bgAlpha))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (active) {
                    val contentColor = if (committed) Color.White else HnColors.Danger
                    val chipBg = if (committed) {
                        Color.White.copy(alpha = 0.20f)
                    } else {
                        HnColors.Danger.copy(alpha = 0.12f)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "삭제",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                        Spacer(Modifier.size(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(chipBg),
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "삭제",
                                tint = contentColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
        content = { content() },
    )
}

@Composable
private fun NotificationRow(n: Notification) {
    val catLabel = when (n.category) {
        NotificationCategory.FLUID   -> "수액"
        NotificationCategory.ORDER   -> "의사오더"
        NotificationCategory.REQUEST -> "환자요청"
        NotificationCategory.WATCH   -> "워치"
    }
    val tagColors = HnColors.notificationTagColors.getValue(n.category)
    val timeLabel = when {
        n.minutesAgo == 0 -> "지금"
        n.minutesAgo in 1..29 -> "${n.minutesAgo}분 전"
        else -> n.time
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.SurfaceAlt)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (n.upcoming) HnColors.Danger else Color.Transparent),
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(catLabel, fg = tagColors.fg, bg = tagColors.bg)
                    if (n.patient.isNotBlank()) {
                        Spacer(Modifier.size(6.dp))
                        Text(n.patient, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    }
                    if (n.room.isNotBlank()) {
                        Spacer(Modifier.size(4.dp))
                        Text(n.room, fontSize = 12.sp, color = HnColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(n.text, fontSize = 13.sp, color = HnColors.Text)
            }
            Text(
                timeLabel,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = HnColors.Text,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
