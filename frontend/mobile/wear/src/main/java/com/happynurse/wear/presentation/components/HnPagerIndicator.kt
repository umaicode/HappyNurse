// HnPagerIndicator — home/record 페이지 인디케이터(도트 2개).
package com.happynurse.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun HnPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(if (active) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
