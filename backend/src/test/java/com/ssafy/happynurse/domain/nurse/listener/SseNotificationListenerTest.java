package com.ssafy.happynurse.domain.nurse.listener;

import com.ssafy.happynurse.domain.nurse.dto.SseNotificationPayload;
import com.ssafy.happynurse.domain.nurse.service.SseEmitterManager;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SseNotificationListenerTest {

    @Mock
    SseEmitterManager sseEmitterManager;

    @InjectMocks
    SseNotificationListener sseNotificationListener;

    @Test
    @DisplayName("담당 간호사가 있으면 sendTo(practitionerId) 호출")
    void onSymptomSubmitted_sendsToAssignedPractitioner() {
        // given
        SymptomSubmittedEvent event = new SymptomSubmittedEvent(
                10L,             // assignedPractitionerId
                1L,              // patientId
                "김가민",        // patientName
                "301호",         // roomName
                "드레싱 교체",   // symptomText
                42L,             // selfReportId
                LocalDateTime.of(2026, 4, 27, 10, 30, 0)
        );

        // when
        sseNotificationListener.onSymptomSubmitted(event);

        // then
        ArgumentCaptor<Long> practitionerIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<SseNotificationPayload> payloadCaptor =
                ArgumentCaptor.forClass(SseNotificationPayload.class);

        verify(sseEmitterManager).sendTo(practitionerIdCaptor.capture(), payloadCaptor.capture());

        assertThat(practitionerIdCaptor.getValue()).isEqualTo(10L);
        assertThat(payloadCaptor.getValue().getPatientName()).isEqualTo("김가민");
        assertThat(payloadCaptor.getValue().getRoomName()).isEqualTo("301호");
        assertThat(payloadCaptor.getValue().getSymptomText()).isEqualTo("드레싱 교체");
        assertThat(payloadCaptor.getValue().getSelfReportId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("담당 간호사가 없으면 sendTo 호출하지 않음")
    void onSymptomSubmitted_skipsWhenNoAssignedPractitioner() {
        // given
        SymptomSubmittedEvent event = new SymptomSubmittedEvent(
                null,   // assignedPractitionerId 없음
                2L, "이환자", "502호", "통증", 55L,
                LocalDateTime.now()
        );

        // when
        sseNotificationListener.onSymptomSubmitted(event);

        // then
        verify(sseEmitterManager, never()).sendTo(any(), any());
    }
}