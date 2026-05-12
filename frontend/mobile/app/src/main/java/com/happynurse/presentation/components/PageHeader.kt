// 페이지 상단 헤더 — 제목 + 보조설명 + 우측 슬롯(보통 알림 아이콘)
package com.happynurse.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.theme.HnColors

@Composable
fun PageHeader(
    title: String,
    sub: String? = null,
    right: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            if (sub != null) {
                Text(sub, fontSize = 16.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (right != null) Box { right() }
    }
}
