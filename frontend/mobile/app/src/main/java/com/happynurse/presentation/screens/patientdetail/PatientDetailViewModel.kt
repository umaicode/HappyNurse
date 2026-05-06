// 환자 상세 ViewModel — 환자 단건/간호일지(날짜별)/의사오더 + 담당 환자 드롭다운 목록
package com.happynurse.presentation.screens.patientdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.repository.EncounterRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val encounterRepository: EncounterRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _myPatients = MutableStateFlow<List<Patient>>(emptyList())
    val myPatients: StateFlow<List<Patient>> = _myPatients.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (initialId > 0L) loadPatient(initialId)
        loadMyPatients()
    }

    fun loadPatient(patientId: Long) {
        viewModelScope.launch {
            patientRepository.getPatient(patientId).fold(
                onSuccess = { p ->
                    _patient.value = p
                    if (p.encounterId > 0L) {
                        loadNotes(p.encounterId, _selectedDate.value)
                        loadOrders(p.encounterId)
                    }
                },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        val eid = _patient.value?.encounterId ?: return
        if (eid > 0L) loadNotes(eid, date)
    }

    private fun loadNotes(encounterId: Long, date: LocalDate) {
        viewModelScope.launch {
            encounterRepository.getNursingNotes(encounterId, date.toString()).fold(
                onSuccess = { _notes.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    private fun loadOrders(encounterId: Long) {
        viewModelScope.launch {
            encounterRepository.getOrders(encounterId).fold(
                onSuccess = { _orders.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    private fun loadMyPatients() {
        viewModelScope.launch {
            patientRepository.getMyWardPatients().fold(
                onSuccess = { list -> _myPatients.value = list.filter { it.isMyPatient } },
                onFailure = { _error.value = it.message },
            )
        }
    }
}
