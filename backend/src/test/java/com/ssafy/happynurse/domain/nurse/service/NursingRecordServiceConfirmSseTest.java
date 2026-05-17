package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecordFactory;
import com.ssafy.happynurse.domain.nurse.event.NursingRecordSavedEvent;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NursingRecordServiceConfirmSseTest {

    @Mock NursingRecordRepository nursingRecordRepository;
    @Mock EncounterRepository encounterRepository;
    @Mock PractitionerRepository practitionerRepository;
    @Mock NursingRecordFactory nursingRecordFactory;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks NursingRecordService nursingRecordService;

    @Test
    @DisplayName("confirm() 성공 시 NursingRecordSavedEvent가 발행된다")
    void confirm_publishesNursingRecordSavedEvent() {
        NursingRecord draft = mock(NursingRecord.class);
        when(draft.getPatientId()).thenReturn(20L);
        when(draft.getAuthorPractitionerId()).thenReturn(200L);
        when(draft.getStatus()).thenReturn(RecordStatus.draft);
        when(draft.getEditContent()).thenReturn("혈압 측정 완료");
        when(draft.getConfirmedAt()).thenReturn(LocalDateTime.now());

        given(nursingRecordRepository.findById(7L)).willReturn(Optional.of(draft));

        nursingRecordService.confirm(7L, 200L);

        ArgumentCaptor<NursingRecordSavedEvent> captor =
                ArgumentCaptor.forClass(NursingRecordSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NursingRecordSavedEvent event = captor.getValue();
        assertThat(event.getNursingRecordId()).isEqualTo(7L);
        assertThat(event.getPatientId()).isEqualTo(20L);
        assertThat(event.getAuthorPractitionerId()).isEqualTo(200L);
    }
}