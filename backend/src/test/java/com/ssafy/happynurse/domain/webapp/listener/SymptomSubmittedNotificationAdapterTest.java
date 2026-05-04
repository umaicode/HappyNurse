package com.ssafy.happynurse.domain.webapp.listener;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomSubmittedNotificationAdapterTest {

    @Mock NotificationDispatcher dispatcher;
    @Mock EncounterRepository encounterRepository;

    @InjectMocks
    SymptomSubmittedNotificationAdapter adapter;

    @Test
    @DisplayName("증상 이벤트를 ASSIGN_DELIVERY envelope으로 변환하여 dispatch")
    void onSymptomSubmitted_dispatchesAssignDeliveryEnvelope() {
        when(encounterRepository.findCurrentWardIdByPatientId(1L)).thenReturn(Optional.of(3L));

        SymptomSubmittedEvent event = new SymptomSubmittedEvent(
                10L, 1L, "이승연", "302호", "두통이 심해요", 42L,
                LocalDateTime.of(2026, 5, 3, 10, 30, 0));

        adapter.on(event);

        ArgumentCaptor<NotificationEnvelope> envCaptor =
                ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(dispatcher).dispatch(envCaptor.capture());

        NotificationEnvelope env = envCaptor.getValue();
        assertThat(env.sourceType()).isEqualTo(SourceType.self_report);
        assertThat(env.wardId()).isEqualTo(3L);
        assertThat(env.assignedPractitionerId()).isEqualTo(10L);
        assertThat(env.patientId()).isEqualTo(1L);
        assertThat(env.sourceEntityId()).isEqualTo(42L);
        assertThat(env.title()).contains("이승연");
        assertThat(env.body()).isEqualTo("두통이 심해요");
        assertThat(env.pushPolicy()).isEqualTo(PushPolicy.ASSIGN_DELIVERY);
    }

    @Test
    @DisplayName("환자의 활성 입원(wardId)을 못 찾으면 dispatch 스킵 (로그만)")
    void onSymptomSubmitted_noWardId_skipsDispatch() {
        when(encounterRepository.findCurrentWardIdByPatientId(1L)).thenReturn(Optional.empty());

        SymptomSubmittedEvent event = new SymptomSubmittedEvent(
                10L, 1L, "이승연", "302호", "두통이 심해요", 42L, LocalDateTime.now());

        adapter.on(event);

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("담당 간호사가 null이면 dispatch 스킵")
    void onSymptomSubmitted_nullPractitioner_skipsDispatch() {
        SymptomSubmittedEvent event = new SymptomSubmittedEvent(
                null, 1L, "이승연", "302호", "두통이 심해요", 42L, LocalDateTime.now());

        adapter.on(event);

        verify(dispatcher, never()).dispatch(any());
    }
}