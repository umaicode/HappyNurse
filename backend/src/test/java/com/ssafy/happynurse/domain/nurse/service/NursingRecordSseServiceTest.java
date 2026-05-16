package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemType;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NursingRecordSseServiceTest {

    @Mock WardEmitterRegistry wardRegistry;
    @Mock NursingRecordRepository nursingRecordRepository;
    @Mock NursingNoteService nursingNoteService;
    @Mock EncounterRepository encounterRepository;

    @InjectMocks NursingRecordSseService sseService;

    @Test
    @DisplayName("활성 입원이 있으면 wardRegistry.send()를 호출한다 (draft → createdAt 사용)")
    void send_callsWardRegistry_whenActiveEncounterExists() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 30, 0);
        NursingRecord record = mock(NursingRecord.class);
        when(record.getPatientId()).thenReturn(10L);
        when(record.getAuthorPractitionerId()).thenReturn(100L);
        when(record.getConfirmedAt()).thenReturn(null);   // draft 상태
        when(record.getCreatedAt()).thenReturn(createdAt);

        given(nursingRecordRepository.findById(1L)).willReturn(Optional.of(record));
        given(encounterRepository.findCurrentWardIdByPatientId(10L)).willReturn(Optional.of(5L));
        given(nursingNoteService.buildSttItem(record, null)).willReturn(mockPayload());

        sseService.send(1L);

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(wardRegistry).send(eq(5L), captor.capture());

        NotificationEnvelope envelope = captor.getValue();
        assertThat(envelope.sourceType()).isEqualTo(SourceType.nursing_record);
        assertThat(envelope.wardId()).isEqualTo(5L);
        assertThat(envelope.patientId()).isEqualTo(10L);
        assertThat(envelope.sourceEntityId()).isEqualTo(1L);
        assertThat(envelope.notificationId()).isNull();
        // confirmedAt이 null이면 createdAt이 envelope.occurredAt에 들어간다
        assertThat(envelope.occurredAt())
                .isEqualTo(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("confirmed 상태면 envelope.occurredAt이 confirmedAt에서 채워진다")
    void send_usesConfirmedAt_whenStatusConfirmed() {
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 5, 15, 11, 0, 0);
        NursingRecord record = mock(NursingRecord.class);
        when(record.getPatientId()).thenReturn(10L);
        when(record.getAuthorPractitionerId()).thenReturn(100L);
        when(record.getConfirmedAt()).thenReturn(confirmedAt);

        given(nursingRecordRepository.findById(1L)).willReturn(Optional.of(record));
        given(encounterRepository.findCurrentWardIdByPatientId(10L)).willReturn(Optional.of(5L));
        given(nursingNoteService.buildSttItem(record, null)).willReturn(mockPayload());

        sseService.send(1L);

        ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(wardRegistry).send(eq(5L), captor.capture());
        assertThat(captor.getValue().occurredAt())
                .isEqualTo(confirmedAt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("활성 입원이 없으면 wardRegistry.send()를 호출하지 않는다")
    void send_skipsWardRegistry_whenNoActiveEncounter() {
        NursingRecord record = mock(NursingRecord.class);
        when(record.getPatientId()).thenReturn(10L);

        given(nursingRecordRepository.findById(1L)).willReturn(Optional.of(record));
        given(encounterRepository.findCurrentWardIdByPatientId(10L)).willReturn(Optional.empty());

        sseService.send(1L);

        verify(wardRegistry, never()).send(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 recordId면 CustomException을 던진다")
    void send_throwsCustomException_whenRecordNotFound() {
        given(nursingRecordRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sseService.send(99L))
                .isInstanceOf(CustomException.class);
    }

    private NursingNoteItemResponse mockPayload() {
        return new NursingNoteItemResponse(
                NursingNoteItemType.STT_NOTE, LocalDateTime.now(),
                RecordStatus.draft, 100L, "김간호", false,
                1L, "환자 혈압 상승", null, null, null);
    }
}