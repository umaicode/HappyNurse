package com.ssafy.happynurse.domain.nurse.notification.dto;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationListItemResponseTest {

    @Test
    void from_mapsAllFieldsFromEntity() {
        // given — patient/practitioner 가 LAZY 라 mock 으로 stub
        Patient patient = mock(Patient.class);
        when(patient.getPatientId()).thenReturn(3L);
        when(patient.getName()).thenReturn("이승연");

        Practitioner practitioner = mock(Practitioner.class);
        when(practitioner.getPractitionerId()).thenReturn(2L);

        PatientSelfReport selfReport = mock(PatientSelfReport.class);
        when(selfReport.getSelfReportId()).thenReturn(17L);

        Notification entity = Notification.create(
                practitioner, SourceType.self_report, selfReport, patient,
                "이승연님의 증상 알림", "드레싱 교체 - 열이 납니다");
        ReflectionTestUtils.setField(entity, "notificationId", 42L);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 5, 3, 14, 39, 25));

        // when
        NotificationListItemResponse response = NotificationListItemResponse.from(entity);

        // then
        assertThat(response.notificationId()).isEqualTo(42L);
        assertThat(response.sourceType()).isEqualTo("self_report");
        assertThat(response.title()).isEqualTo("이승연님의 증상 알림");
        assertThat(response.body()).isEqualTo("드레싱 교체 - 열이 납니다");
        assertThat(response.patientId()).isEqualTo(3L);
        assertThat(response.patientName()).isEqualTo("이승연");
        assertThat(response.sourceEntityId()).isEqualTo(17L);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 3, 14, 39, 25));
        assertThat(response.recipientPractitionerId()).isEqualTo(2L);
    }

    @Test
    void from_handlesNullPatient() {
        Practitioner practitioner = mock(Practitioner.class);
        when(practitioner.getPractitionerId()).thenReturn(2L);

        // patient null + selfReport null (예: timer)
        Notification entity = Notification.create(
                practitioner, SourceType.timer, null, null,
                "타이머", "투약 시간 알림");
        ReflectionTestUtils.setField(entity, "notificationId", 50L);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());

        NotificationListItemResponse response = NotificationListItemResponse.from(entity);

        assertThat(response.patientId()).isNull();
        assertThat(response.patientName()).isNull();
        assertThat(response.sourceEntityId()).isNull();
        assertThat(response.recipientPractitionerId()).isEqualTo(2L);
    }
}