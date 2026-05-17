// 알림 바텀시트 — 카테고리별 색칩 + 긴급(빨간 점) + 시간 라벨
package com.happynurse.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.Notification
import com.happynurse.domain.model.NotificationCategory
import com.happynurse.domain.model.NotificationPriority
import com.happynurse.presentation.theme.HnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    visible: Boolean,
    notifications: List<Notification>,
    onClose: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = HnColors.Surface,
    ) {
      Column(modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.8f)) {
        // 과거 24h ~ 미래 24h 알림만 표시 (워치알람은 minutesAgo 가 음수)
        val recent = notifications.filter { it.minutesAgo in -1440..1440 }
        Text(
            "알림",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HnColors.Text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
        )
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
                    // 신규 진입 시 페이드 + 위에서 슬라이드 다운 애니메이션.
                    // remember 가 첫 컴포지션에서 false 로 캡처된 뒤 LaunchedEffect 로 true 전환 —
                    // LazyColumn 안에서 신규 item 이 마운트될 때마다 enter 트랜지션 보장.
                    val visibleState = remember {
                        MutableTransitionState(initialState = false)
                    }
                    LaunchedEffect(notification.id) { visibleState.targetState = true }
                    AnimatedVisibility(
                        visibleState = visibleState,
                        modifier = Modifier.animateItem(),
                        enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300)) { -it / 2 } +
                            expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) +
                            shrinkVertically(animationSpec = tween(200)),
                    ) {
                        NotificationRow(notification)
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
      } // end inner Column (시트 최대 높이 70% 강제)
    }
}

@Composable
private fun NotificationRow(n: Notification) {
    val catLabel = when (n.category) {
        NotificationCategory.FLUID   -> "수액"
        NotificationCategory.ORDER   -> "의사오더변경"
        NotificationCategory.REQUEST -> "환자요청"
        NotificationCategory.WATCH   -> "워치"
        NotificationCategory.SESSION -> "웹"
    }
    val tagColors = HnColors.notificationTagColors.getValue(n.category)
    val timeLabel = when {
        n.minutesAgo == 0 -> "지금"
        n.minutesAgo in 1..59 -> "${n.minutesAgo}분 전"
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
                    if (n.category == NotificationCategory.REQUEST && n.priority != null) {
                        Spacer(Modifier.size(6.dp))
                        PriorityChip(n.priority)
                    }
                    if (n.patient.isNotBlank()) {
                        Spacer(Modifier.size(8.dp))
                        Text(n.patient, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    }
                    if (n.room.isNotBlank()) {
                        Spacer(Modifier.size(6.dp))
                        Text(n.room, fontSize = 14.sp, color = HnColors.TextSecondary)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        timeLabel,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = HnColors.Text,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(n.text, fontSize = 17.sp, color = HnColors.Text)
            }
        }
    }
}

// 환자요청 긴급도 칩 — critical 만 경고 아이콘 동반
@Composable
private fun PriorityChip(priority: NotificationPriority) {
    val (label, fg, bg) = when (priority) {
        NotificationPriority.CRITICAL -> Triple("위급",   HnColors.Danger,        Color(0xFFFDECEC))
        NotificationPriority.HIGH     -> Triple("높음",   Color(0xFFE1685F),      Color(0xFFFDECEC))
        NotificationPriority.MEDIUM   -> Triple("보통",   Color(0xFFD3640F),      Color(0xFFFEFAED))
        NotificationPriority.LOW      -> Triple("낮음",   Color(0xFF166534),      Color(0xFFECFDF3))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        if (priority == NotificationPriority.CRITICAL) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.size(3.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 14.sp,
            fontWeight = if (priority == NotificationPriority.CRITICAL) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
