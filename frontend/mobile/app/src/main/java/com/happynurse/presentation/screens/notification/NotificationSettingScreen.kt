// 알림 설정 화면 — IV/타이머/오더/투약오류 등 알림 유형별 토글 설정
package com.happynurse.presentation.screens.notification

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

// 알림_046: 알림 유형별(수액, 타이머, 오더 등) 수신 여부 개인 설정 관리
@Composable
fun NotificationSettingScreen(
    navController: NavController,
    viewModel: NotificationSettingViewModel = hiltViewModel()
) {
    // TODO: 알림 설정 UI 구현
}
