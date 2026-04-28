package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_practitioner_id", nullable = false)
    private Practitioner recipientPractitioner; // 수신 간호사

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType; // 알림 타입

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_self_report_id")
    private PatientSelfReport sourceSelfReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient; // 대상 환자

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification create(Practitioner recipientPractitioner, SourceType sourceType, PatientSelfReport patientSelfReport, Patient patient, String title, String body) {
        Notification notification = new Notification();
        notification.recipientPractitioner = recipientPractitioner;
        notification.sourceType = sourceType;
        notification.sourceSelfReport = patientSelfReport;
        notification.patient = patient;
        notification.title = title;
        notification.body = body;

        return notification;
    }
}