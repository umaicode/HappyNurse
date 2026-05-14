package com.ssafy.happynurse.domain.watch.listener;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.event.IvAlertEvent;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import com.ssafy.happynurse.domain.watch.scheduler.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

/**
 * 이벤트 받음 -> 누구테, 어떤 메세지(IvAlertEvet -> NotificationEnvelope) 넘길지 결정하고 dispatcher 넘김
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IvAlertNotificationAdapter {

    private final IvInfusionRepository ivInfusionRepository;
    private final NotificationDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IvAlertEvent event) {
        IvInfusion iv = ivInfusionRepository.findByIdWithRoutingInfo(event.ivInfusionId())
                .orElse(null);
        if (iv == null) {
            log.warn("IV alert dispatch skip: infusion not found. infusionId={}, alertType={}",
                    event.ivInfusionId(), event.alertType());
            return;
        }

        Practitioner nurse = iv.getEncounter().getAssignedPractitioner();
        if (nurse == null) {
            log.warn("IV alert dispatch skip: 담당 간호사 미배정. infusionId={}, patientId={}, alertType={}",
                    event.ivInfusionId(), iv.getPatient().getPatientId(), event.alertType());
            return;
        }

        Long wardId = iv.getEncounter().getRoom().getWard().getWardId();
        Long patientId = iv.getPatient().getPatientId();
        Long recipientId = nurse.getPractitionerId();

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.iv_alert,
                wardId,
                recipientId,
                patientId,
                iv.getIvInfusionId(),     // sourceEntityId — Notification.source_iv_infusion_id 채움
                titleOf(event.alertType()),
                bodyOf(event.alertType(), iv),
                event,                     // payload — SSE/FCM 직렬화 (DB 저장 X)
                event.firedAt(),
                null,
                PushPolicy.ASSIGN_DELIVERY,
                null   // priority — iv_alert는 자가보고 분류 대상 아님
        );

        dispatcher.dispatch(envelope);
        long delayMs = Math.max(0L, Duration.between(event.firedAt(), Instant.now()).toMillis());
        log.info("IV alert dispatched: infusionId={}, alertType={}, recipientId={}, wardId={}, delay_ms={}",
                event.ivInfusionId(), event.alertType(), recipientId, wardId, delayMs);
    }

    private String titleOf(AlertType type) {
        return switch (type) {
            case FIVE_MIN_BEFORE -> "수액 종료 5분 전";
            case COMPLETED       -> "수액 종료";
        };
    }

    private String bodyOf(AlertType type, IvInfusion iv) {
        String medLabel = formatMedicationLabel(iv);
        return switch (type) {
            case FIVE_MIN_BEFORE -> String.format("%s 수액 종료 5분 전", medLabel);
            case COMPLETED       -> String.format("%s 수액 종료", medLabel);
        };
    }

    /**
     * mix IV 표시 — 1개면 그대로, 2개 이상이면 "primary 외 N건 혼합"
     */
    private String formatMedicationLabel(IvInfusion iv) {
        var meds = iv.getMedications();
        if (meds.isEmpty()) {
            return "(약물 미지정)";
        }
        String primary = meds.get(0).getMedication().getProductName();
        if (meds.size() == 1) {
            return primary;
        }
        return String.format("%s 외 %d건 혼합", primary, meds.size() - 1);
    }
}
