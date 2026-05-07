// 환자 탭 ViewModel — 병동 환자 목록 조회 / 담당환자 일괄 저장
package com.happynurse.presentation.screens.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class PatientsViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val practitionerRepository: PractitionerRepository,
) : ViewModel() {

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _profile = MutableStateFlow<NurseProfile?>(null)
    val profile: StateFlow<NurseProfile?> = _profile.asStateFlow()

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
                onSuccess = { _patients.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun saveAssignment(encounterIds: Set<Long>) {
        viewModelScope.launch {
            practitionerRepository.updateAssignedPatients(encounterIds.toList()).fold(
                onSuccess = { load() },
                onFailure = { _error.value = it.message },
            )
        }
    }
}
