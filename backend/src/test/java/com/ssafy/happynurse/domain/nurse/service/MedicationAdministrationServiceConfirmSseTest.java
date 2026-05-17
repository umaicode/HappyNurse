package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.ssafy.happynurse.domain.watch.entity.Medication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationAdministrationServiceConfirmSseTest {

    @Mock MedicationAdministrationRepository medicationAdministrationRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MedicationAdministrationService medicationAdministrationService;

    @Test
    @DisplayName("confirm() 성공 시 MedicationAdministrationSavedEvent가 발행된다")
    void confirm_publishesMedicationAdministrationSavedEvent() {
        // given
        String taggingId = "tag-uuid-1";
        Long me = 6L;

        Practitioner practitioner = mock(Practitioner.class);
        when(practitioner.getPractitionerId()).thenReturn(me);

        Encounter encounter = mock(Encounter.class);
        when(encounter.getEncounterId()).thenReturn(42L);

        Patient patient = mock(Patient.class);
        when(patient.getPatientId()).thenReturn(7L);

        Medication medication = mock(Medication.class);

        MedicationAdministration entity = mock(MedicationAdministration.class);
        when(entity.getPractitioner()).thenReturn(practitioner);
        when(entity.getEncounter()).thenReturn(encounter);
        when(entity.getPatient()).thenReturn(patient);
        when(entity.getStatus()).thenReturn(RecordStatus.draft);
        when(entity.getMedication()).thenReturn(medication);

        // findAllByTaggingId 는 confirm() 안에서 2번 호출됨 (loadGroupAndAuthorize 1번 + refreshed 1번)
        // 같은 mock entity 가 양쪽에 반환되어도 이 테스트의 검증 목적(이벤트 발행)에는 영향 없음
        given(medicationAdministrationRepository.findAllByTaggingId(taggingId))
                .willReturn(List.of(entity));

        // when
        medicationAdministrationService.confirm(taggingId, me);

        // then
        ArgumentCaptor<MedicationAdministrationSavedEvent> captor =
                ArgumentCaptor.forClass(MedicationAdministrationSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        MedicationAdministrationSavedEvent event = captor.getValue();
        assertThat(event.getTaggingId()).isEqualTo(taggingId);
        assertThat(event.getEncounterId()).isEqualTo(42L);
        assertThat(event.getPatientId()).isEqualTo(7L);
        assertThat(event.getAuthorPractitionerId()).isEqualTo(me);
    }
}