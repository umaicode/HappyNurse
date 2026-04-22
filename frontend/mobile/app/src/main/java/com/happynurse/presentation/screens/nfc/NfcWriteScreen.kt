package com.happynurse.presentation.screens.nfc

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

// NFC_028: 환자 팔찌 NFC 라이팅 (관리자 전용)
// NFC_031: 약물 NFC 라이팅 (관리자/약사 전용)
@Composable
fun NfcWriteScreen(
    navController: NavController,
    viewModel: NfcWriteViewModel = hiltViewModel()
) {
    // TODO: NFC 라이팅 UI 구현 (환자 팔찌 / 약물 태그 선택 → 데이터 입력 → AES-256 암호화 후 라이팅)
}
