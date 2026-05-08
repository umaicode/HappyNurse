package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.notification.repository.NotificationRepository;
import com.ssafy.happynurse.domain.nurse.notification.service.fcm.FcmSender;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.PersonalEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.reminder.repository.SttReminderRepository;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationDispatcherTest {

    @Mock WardEmitterRegistry wardRegistry;
    @Mock PersonalEmitterRegistry personalRegistry;
    @Mock FcmSender fcmSender;
    @Mock NotificationRepository notificationRepository;
    @Mock PractitionerRepository practitionerRepository;
    @Mock PatientRepository patientRepository;
    @Mock PatientSelfReportRepository patientSelfReportRepository;
    @Mock SttReminderRepository sttReminderRepository;

    @InjectMocks
    DefaultNotificationDispatcher dispatcher;

    @Test
    void dispatch_nullWardId_throwsIllegalArgument() {
        NotificationEnvelope env = envelope(null, 7L, PushPolicy.ASSIGN_DELIVERY, SourceType.self_report);
        assertThatThrownBy(() -> dispatcher.dispatch(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wardId");
    }

    @Test
    void dispatch_nullPractitioner_throwsIllegalArgument() {
        NotificationEnvelope env = envelope(1L, null, PushPolicy.ASSIGN_DELIVERY, SourceType.self_report);
        assertThatThrownBy(() -> dispatcher.dispatch(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assignedPractitionerId");
    }

    @Test
    void dispatch_persistsNotificationAndRoutesAllChannels() {
        stubMinimalLookups();
        Notification saved = stubSavedNotification();   // ← 분리
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationEnvelope env = envelope(1L, 7L, PushPolicy.ASSIGN_DELIVERY, SourceType.self_report);
        dispatcher.dispatch(env);

        verify(notificationRepository).save(any(Notification.class));

        ArgumentCaptor<NotificationEnvelope> sentEnv = ArgumentCaptor.forClass(NotificationEnvelope.class);
        verify(wardRegistry).send(eq(1L), sentEnv.capture());
        assertThat(sentEnv.getValue().notificationId()).isEqualTo(999L);

        verify(personalRegistry).send(eq(7L), any(NotificationEnvelope.class));
        verify(fcmSender).sendToActiveDevicesOf(eq(7L), any(NotificationEnvelope.class));
    }

    @Test
    void dispatch_personalInfo_skipsWardSse_keepsPersonalAndFcm() {
        stubMinimalLookups();
        Notification saved = stubSavedNotification();   // ← 분리
        when(notificationRepository.save(any())).thenReturn(saved);

        NotificationEnvelope env = envelope(1L, 7L, PushPolicy.PERSONAL_INFO, SourceType.timer);
        dispatcher.dispatch(env);

        verify(wardRegistry, never()).send(any(), any());
        verify(personalRegistry).send(eq(7L), any());
        verify(fcmSender).sendToActiveDevicesOf(eq(7L), any());
    }

    @Test
    void dispatch_wardChannelThrows_personalAndFcmStillExecute() {
        stubMinimalLookups();
        Notification saved = stubSavedNotification();   // ← 분리
        when(notificationRepository.save(any())).thenReturn(saved);
        doThrow(new RuntimeException("ward boom"))
                .when(wardRegistry).send(any(), any());

        NotificationEnvelope env = envelope(1L, 7L, PushPolicy.ASSIGN_DELIVERY, SourceType.self_report);
        dispatcher.dispatch(env);

        verify(personalRegistry).send(eq(7L), any());
        verify(fcmSender).sendToActiveDevicesOf(eq(7L), any());
    }

    @Test
    void dispatch_personalChannelThrows_fcmStillExecutes() {
        stubMinimalLookups();
        Notification saved = stubSavedNotification();   // ← 분리
        when(notificationRepository.save(any())).thenReturn(saved);
        doThrow(new RuntimeException("personal boom"))
                .when(personalRegistry).send(any(), any());

        NotificationEnvelope env = envelope(1L, 7L, PushPolicy.ASSIGN_DELIVERY, SourceType.self_report);
        dispatcher.dispatch(env);

        verify(fcmSender).sendToActiveDevicesOf(eq(7L), any());
    }

    private void stubMinimalLookups() {
        when(practitionerRepository.findById(7L)).thenReturn(Optional.of(mock(Practitioner.class)));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(mock(Patient.class)));
        org.mockito.Mockito.lenient()
                .when(patientSelfReportRepository.findById(50L))
                .thenReturn(Optional.of(mock(PatientSelfReport.class)));
        org.mockito.Mockito.lenient()
                .when(sttReminderRepository.findById(50L))
                .thenReturn(Optional.of(mock(SttReminder.class)));
    }

    private Notification stubSavedNotification() {
        Notification n = mock(Notification.class);
        when(n.getNotificationId()).thenReturn(999L);
        return n;
    }

    private static <T> T mock(Class<T> c) {
        return org.mockito.Mockito.mock(c);
    }

    private NotificationEnvelope envelope(Long wardId, Long practitionerId, PushPolicy policy, SourceType sourceType) {
        return new NotificationEnvelope(
                sourceType,
                wardId, practitionerId, 100L, 50L,
                "title", "body",
                "payload", Instant.now(), null,
                policy);
    }
}