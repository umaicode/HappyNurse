package com.ssafy.happynurse.domain.nurse.notification.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
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
    @JoinColumn(name = "source_iv_infusion_id")
    private IvInfusion sourceIvInfusion; // sourceType=iv_alert 일 때만 채워짐

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_stt_reminder_id")
    private SttReminder sourceSttReminder; // sourceType=timer 일 때만 채워짐

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient; // 대상 환자

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SymptomPriority priority; // 자가보고 알림 한정. iv_alert/order_change 등은 null.

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

    /** 자가보고 알림 — priority 포함. */
    public static Notification create(Practitioner recipientPractitioner, SourceType sourceType, PatientSelfReport patientSelfReport, Patient patient, String title, String body, SymptomPriority priority) {
        Notification notification = create(recipientPractitioner, sourceType, patientSelfReport, patient, title, body);
        notification.priority = priority;
        return notification;
    }

    /** 수액 알림용 — sourceType=iv_alert 고정. */
    public static Notification createForIvAlert(
            Practitioner recipientPractitioner,
            IvInfusion sourceIvInfusion,
            Patient patient,
            String title,
            String body) {
        Notification notification = new Notification();
        notification.recipientPractitioner = recipientPractitioner;
        notification.sourceType = SourceType.iv_alert;
        notification.sourceIvInfusion = sourceIvInfusion;
        notification.patient = patient;
        notification.title = title;
        notification.body = body;
        return notification;
    }

    /** STT 음성 메모 타이머 알람용 — sourceType=timer 고정. */
    public static Notification createForSttReminder(
            Practitioner recipientPractitioner,
            SttReminder sourceSttReminder,
            Patient patient,
            String title,
            String body) {
        Notification notification = new Notification();
        notification.recipientPractitioner = recipientPractitioner;
        notification.sourceType = SourceType.timer;
        notification.sourceSttReminder = sourceSttReminder;
        notification.patient = patient;
        notification.title = title;
        notification.body = body;
        return notification;
    }
}