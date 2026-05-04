// 로그인 화면 ViewModel(Hilt) — 인증 로직 미구현
package com.happynurse.presentation.screens.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    // TODO 백엔드 로그인 API 호출 + JWT 저장 (DataStore) + FcmTokenRegistrar.register(jwt) 호출
    //  로그인 성공 후 nfcToken navArg 가 있으면 nfc_entry/{token} 으로 navigate, 없으면 patient_list 로
}