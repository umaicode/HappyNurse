// HnSegmentedTabs — 홈 화면 상단의 3개 segmented tab(수액/타이머/환자알림).
// 선택된 탭은 primary 컬러 배경, 미선택은 surfaceContainerHigh 톤.
package com.happynurse.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

data class HnTab(val label: String, val count: Int)

@Composable
fun HnSegmentedTabs(
    tabs: List<HnTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelected(index) }
                    .padding(PaddingValues(horizontal = 4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tab.count > 0) "${tab.label} ${tab.count}" else tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = fg,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
