// 환자 탭 ViewModel — 병동 환자 목록 조회 / 담당환자 일괄 저장
package com.happynurse.presentation.screens.patients

import com.happynurse.data.repository.PatientRepository
import com.happynurse.data.repository.PractitionerRepository
import com.happynurse.domain.model.NurseProfile
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val practitionerRepository: PractitionerRepository,
) : BaseViewModel() {

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _profile = MutableStateFlow<NurseProfile?>(null)
    val profile: StateFlow<NurseProfile?> = _profile.asStateFlow()

    init { load() }

    fun load() {
        launchWithResult(block = { practitionerRepository.getProfile() }) { _profile.value = it }
        launchWithResult(block = { patientRepository.getMyWardPatients() }) { _patients.value = it }
    }

    fun saveAssignment(encounterIds: Set<Long>) {
        launchWithResult(block = { practitionerRepository.updateAssignedPatients(encounterIds.toList()) }) {
            load()
        }
    }
}
