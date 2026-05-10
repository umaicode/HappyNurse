// 알림 바텀시트 — 카테고리별 색칩 + 긴급(빨간 점) + 시간 라벨
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.Notif
import com.happynurse.domain.model.NotifCategory
import com.happynurse.presentation.theme.HnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    visible: Boolean,
    notifications: List<Notif>,
    onClose: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = HnColors.Surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
        ) {
            Text("알림", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text, modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = HnColors.TextSecondary)
            }
        }
        // 현재 시간 기준 24시간(1440분) 이내 알림만 표시
        val recent = notifications.filter { it.minutesAgo in 0..1440 }
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
                compareBy<Notif> { it.minutesAgo },
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                items(sorted, key = { it.id }) { NotifRow(it) }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun NotifRow(n: Notif) {
    val (catLabel, catFg, catBg) = when (n.category) {
        NotifCategory.FLUID   -> Triple("수액",     HnColors.Purple, HnColors.TagFluidBg)
        NotifCategory.WATCH   -> Triple("워치",     HnColors.Danger, Color(0xFFFEE2E2))
        NotifCategory.REQUEST -> Triple("환자요청", HnColors.Info,   HnColors.TagInjBg)
    }
    val timeLabel = when {
        n.minutesAgo == 0 -> "지금"
        n.minutesAgo < 0  -> "${-n.minutesAgo}분 후"
        n.minutesAgo < 60 -> "${n.minutesAgo}분 전"
        else              -> n.time
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
                    TagChip(catLabel, fg = catFg, bg = catBg)
                    Spacer(Modifier.size(6.dp))
                    Text(n.patient, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(4.dp))
                    Text(n.room, fontSize = 12.sp, color = HnColors.TextSecondary)
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
