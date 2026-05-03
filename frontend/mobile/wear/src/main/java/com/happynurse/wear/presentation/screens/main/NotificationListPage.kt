package com.happynurse.wear.presentation.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.happynurse.wear.data.notification.WearNotification

@Composable
fun NotificationListPage(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val tabItems = notifications[selectedTab.type].orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                    color = if (tab == selectedTab) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onBackground
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { viewModel.selectTab(tab) },
                )
            }
        }

        if (tabItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "알림 없음",
                    style = MaterialTheme.typography.caption1,
                )
            }
        } else {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tabItems) { notification ->
                    NotificationItemRow(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(notification: WearNotification) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
    ) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${notification.patientName} · ${notification.roomLocation}",
            style = MaterialTheme.typography.caption2,
        )
    }
}
