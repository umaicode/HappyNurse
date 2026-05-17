package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveRequest;
import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationServiceSseTest {

    @Mock NfcTagRepository nfcTagRepository;
    @Mock MedicationOrderRepository medicationOrderRepository;
    @Mock MedicationAdministrationRepository medicationAdministrationRepository;
    @Mock PatientRepository patientRepository;
    @Mock EncounterRepository encounterRepository;
    @Mock PractitionerRepository practitionerRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MedicationService medicationService;

    @Test
    @DisplayName("saveAdministrations() 성공 시 MedicationAdministrationSavedEvent가 발행된다")
    void saveAdministrations_publishesMedicationAdministrationSavedEvent() {
        // given
        Patient patient = mock(Patient.class);
        when(patient.getPatientId()).thenReturn(7L);
        Encounter encounter = mock(Encounter.class);
        when(encounter.getEncounterId()).thenReturn(42L);
        Practitioner practitioner = mock(Practitioner.class);
        when(practitioner.getPractitionerId()).thenReturn(200L);
        Medication medication = mock(Medication.class);

        MedicationOrder order = mock(MedicationOrder.class);
        when(order.getMedicationOrderId()).thenReturn(11L);
        when(order.getStatus()).thenReturn(OrderStatus.active);
        when(order.getPatient()).thenReturn(patient);
        when(order.getMedication()).thenReturn(medication);
        when(order.getDose()).thenReturn(new BigDecimal("5"));
        when(order.getDoseUnit()).thenReturn("mg");

        given(patientRepository.findById(7L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(42L)).willReturn(Optional.of(encounter));
        given(practitionerRepository.findById(200L)).willReturn(Optional.of(practitioner));
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(11L))).willReturn(List.of(order));
        given(medicationAdministrationRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        MedicationAdministrationSaveRequest request =
                new MedicationAdministrationSaveRequest(7L, 42L, List.of(11L));

        // when
        medicationService.saveAdministrations(request, 200L);

        // then
        ArgumentCaptor<MedicationAdministrationSavedEvent> captor =
                ArgumentCaptor.forClass(MedicationAdministrationSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        MedicationAdministrationSavedEvent event = captor.getValue();
        assertThat(event.getTaggingId()).isNotBlank();
        assertThat(event.getEncounterId()).isEqualTo(42L);
        assertThat(event.getPatientId()).isEqualTo(7L);
        assertThat(event.getAuthorPractitionerId()).isEqualTo(200L);
    }
}