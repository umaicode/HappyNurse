package com.ssafy.happynurse.domain.nurse.notification.dto;

import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;

import java.time.LocalDateTime;

public record NotificationListItemResponse(
        Long notificationId,
        String sourceType,
        String title,
        String body,
        Long patientId,
        String patientName,
        Long sourceEntityId,
        LocalDateTime createdAt,
        Long recipientPractitionerId
) {
    public static NotificationListItemResponse from(Notification entity) {
        Patient patient = entity.getPatient();
        PatientSelfReport selfReport = entity.getSourceSelfReport();
        return new NotificationListItemResponse(
                entity.getNotificationId(),
                entity.getSourceType().name(),
                entity.getTitle(),
                entity.getBody(),
                patient == null ? null : patient.getPatientId(),
                patient == null ? null : patient.getName(),
                selfReport == null ? null : selfReport.getSelfReportId(),
                entity.getCreatedAt(),
                entity.getRecipientPractitioner().getPractitionerId()
        );
    }
}