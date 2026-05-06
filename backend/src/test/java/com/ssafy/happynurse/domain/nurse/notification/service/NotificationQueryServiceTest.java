package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListResponse;
import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.notification.repository.NotificationRepository;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    NotificationRepository repository;

    @InjectMocks
    NotificationQueryService service;

    @Test
    void findWardInbox_clampsLimitToMax() {
        when(repository.findByWardIdWithCursor(anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        service.findWardInbox(1L, null, null, 500);  // 500 → 100 으로 클램프

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByWardIdWithCursor(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void findWardInbox_appliesDefaultLimitWhenNull() {
        when(repository.findByWardIdWithCursor(anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        service.findWardInbox(1L, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByWardIdWithCursor(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void findWardInbox_setsNextBeforeWhenItemsReachLimit() {
        Notification n1 = stubNotification(50L);
        Notification n2 = stubNotification(48L);
        when(repository.findByWardIdWithCursor(anyLong(), any(), any(), any()))
                .thenReturn(List.of(n1, n2));

        NotificationListResponse response = service.findWardInbox(1L, null, null, 2);

        assertThat(response.items()).hasSize(2);
        assertThat(response.nextBefore()).isEqualTo(48L);  // 마지막 item 의 id
    }

    @Test
    void findWardInbox_nextBeforeIsNullWhenItemsBelowLimit() {
        Notification n = stubNotification(50L);   // ← 먼저 stub 완료 (변수로 분리)
        when(repository.findByWardIdWithCursor(anyLong(), any(), any(), any()))
                .thenReturn(List.of(n));

        NotificationListResponse response = service.findWardInbox(1L, null, null, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.nextBefore()).isNull();
    }

    @Test
    void findPersonalInbox_callsRepositoryWithPractitionerId() {
        when(repository.findByRecipientPractitionerIdWithCursor(anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        service.findPersonalInbox(7L, null, null, null);

        verify(repository).findByRecipientPractitionerIdWithCursor(eq(7L), isNull(), isNull(), any());
    }

    private Notification stubNotification(long id) {
        Practitioner p = org.mockito.Mockito.mock(Practitioner.class);
        when(p.getPractitionerId()).thenReturn(2L);

        Patient patient = org.mockito.Mockito.mock(Patient.class);
        when(patient.getPatientId()).thenReturn(3L);
        when(patient.getName()).thenReturn("환자");

        Notification n = Notification.create(p, SourceType.self_report, null, patient, "t", "b");
        ReflectionTestUtils.setField(n, "notificationId", id);
        ReflectionTestUtils.setField(n, "createdAt", LocalDateTime.now());
        return n;
    }
}