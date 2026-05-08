package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
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
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private final WardEmitterRegistry wardRegistry;
    private final PersonalEmitterRegistry personalRegistry;
    private final FcmSender fcmSender;

    private final NotificationRepository notificationRepository;
    private final PractitionerRepository practitionerRepository;
    private final PatientRepository patientRepository;
    private final PatientSelfReportRepository patientSelfReportRepository;
    private final IvInfusionRepository ivInfusionRepository;
    private final SttReminderRepository sttReminderRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationEnvelope envelope) {
        validate(envelope);

        // 1) 영속화 — 모든 알림 저장
        Notification saved = notificationRepository.save(toEntity(envelope));
        NotificationEnvelope filled = envelope.withNotificationId(saved.getNotificationId());

        // 2) ward 채널
        if (filled.pushPolicy().isWardSse()) {
            try {
                wardRegistry.send(filled.wardId(), filled);
            } catch (Exception e) {
                log.warn("ward SSE 전송 실패: wardId={}, sourceType={}",
                        filled.wardId(), filled.sourceType(), e);
            }
        }

        // 3) personal 채널
        if (filled.pushPolicy().isPersonalSse()) {
            try {
                personalRegistry.send(filled.assignedPractitionerId(), filled);
            } catch (Exception e) {
                log.warn("personal SSE 전송 실패: practitionerId={}, sourceType={}",
                        filled.assignedPractitionerId(), filled.sourceType(), e);
            }
        }

        // 4) FCM
        if (filled.pushPolicy().isFcm()) {
            try {
                fcmSender.sendToActiveDevicesOf(filled.assignedPractitionerId(), filled);
            } catch (Exception e) {
                log.warn("FCM 전송 실패: practitionerId={}, sourceType={}",
                        filled.assignedPractitionerId(), filled.sourceType(), e);
            }
        }
    }

    private void validate(NotificationEnvelope envelope) {
        if (envelope.wardId() == null) {
            throw new IllegalArgumentException("wardId must not be null");
        }
        if (envelope.sourceType() == null) {
            throw new IllegalArgumentException("sourceType must not be null");
        }
        if (envelope.assignedPractitionerId() == null) {
            throw new IllegalArgumentException("assignedPractitionerId must not be null");
        }
    }

    /**
     * envelope → Notification 엔티티 변환.
     * SourceType 별로 source FK 컬럼이 다르므로 분기 처리. (수액 추가 완료)
     */
    private Notification toEntity(NotificationEnvelope env) {
        Practitioner recipient = practitionerRepository.findById(env.assignedPractitionerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Practitioner not found: " + env.assignedPractitionerId()));

        Patient patient = env.patientId() == null ? null
                : patientRepository.findById(env.patientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found: " + env.patientId()));

        if (env.sourceType() == SourceType.iv_alert && env.sourceEntityId() != null) {
            IvInfusion ivInfusion = ivInfusionRepository.findById(env.sourceEntityId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "IvInfusion not found: " + env.sourceEntityId()));
            return Notification.createForIvAlert(recipient, ivInfusion, patient, env.title(), env.body());
        }

        if (env.sourceType() == SourceType.timer && env.sourceEntityId() != null) {
            SttReminder reminder = sttReminderRepository.findById(env.sourceEntityId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SttReminder not found: " + env.sourceEntityId()));
            return Notification.createForSttReminder(recipient, reminder, patient, env.title(), env.body());
        }

        PatientSelfReport selfReport = (env.sourceType() == SourceType.self_report && env.sourceEntityId() != null)
                ? patientSelfReportRepository.findById(env.sourceEntityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "PatientSelfReport not found: " + env.sourceEntityId()))
                : null;

        if (env.priority() != null) {
            return Notification.create(
                    recipient,
                    env.sourceType(),
                    selfReport,
                    patient,
                    env.title(),
                    env.body(),
                    env.priority()
            );
        }
        return Notification.create(
                recipient,
                env.sourceType(),
                selfReport,
                patient,
                env.title(),
                env.body()
        );
    }
}