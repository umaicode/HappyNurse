// 하단 4탭 네비게이션 바 — 환자/알람/인수인계/마이페이지
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.theme.HnColors

enum class HnTab(val id: String, val label: String, val icon: ImageVector) {
    PATIENTS("patients", "환자", Icons.Outlined.Person),
    ALARMS("alarms", "알람", Icons.Outlined.Notifications),
    HANDOFF("handoff", "인수인계", Icons.Outlined.Assignment),
    ME("me", "마이페이지", Icons.Outlined.PersonOutline),
}

@Composable
fun BottomNav(
    active: HnTab,
    onChange: (HnTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(HnColors.Surface),
    ) {
        HnTab.entries.forEach { tab ->
            val on = tab == active
            val tint: Color = if (on) HnColors.Primary else HnColors.TextTertiary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onChange(tab) }
                    .padding(top = 8.dp, bottom = 6.dp),
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    text = tab.label,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
