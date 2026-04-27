// 워치 메인 화면 — 환자 태깅/약물 태깅/음성 녹음 진입 칩 버튼 리스트
package com.happynurse.wear.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.happynurse.wear.presentation.navigation.WearRoute

// 3-1-1 환자 NFC 태깅 / 3-1-2 약물 NFC 태깅 / 3-2 음성 녹음 진입점
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Chip(
                label = { Text("환자 태깅") },
                onClick = { navController.navigate(WearRoute.PATIENT_TAG) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        item {
            Chip(
                label = { Text("약물 태깅") },
                onClick = { navController.navigate(WearRoute.MEDICATION_TAG) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        item {
            Chip(
                label = { Text("음성 녹음") },
                onClick = { navController.navigate(WearRoute.RECORDING) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}
