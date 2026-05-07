// 마이페이지 ViewModel — 프로필/담당환자 로드, 로그아웃 처리
package com.happynurse.presentation.screens.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.repository.AuthRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.data.repository.PractitionerRepository
import com.happynurse.domain.model.NurseProfile
import com.happynurse.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val practitionerRepository: PractitionerRepository,
    private val patientRepository: PatientRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<NurseProfile?>(null)
    val profile: StateFlow<NurseProfile?> = _profile.asStateFlow()

    private val _myPatients = MutableStateFlow<List<Patient>>(emptyList())
    val myPatients: StateFlow<List<Patient>> = _myPatients.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            practitionerRepository.getProfile().fold(
                onSuccess = { _profile.value = it },
                onFailure = { _error.value = it.message },
            )
            patientRepository.getMyWardPatients().fold(
                onSuccess = { list -> _myPatients.value = list.filter { it.isMyPatient } },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
