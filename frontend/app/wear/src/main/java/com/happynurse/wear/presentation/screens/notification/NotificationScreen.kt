package com.happynurse.wear.presentation.screens.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text

// 알림_043: 수액 알림 / 알림_044: 노트 타이머 알람 (환자 위치 포함)
@Composable
fun NotificationScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notification by viewModel.currentNotification.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        notification?.let {
            Text(it.title)
            Text(it.patientName)
            Text("위치: ${it.roomLocation}")
        } ?: Text("알림 없음")
    }
}
