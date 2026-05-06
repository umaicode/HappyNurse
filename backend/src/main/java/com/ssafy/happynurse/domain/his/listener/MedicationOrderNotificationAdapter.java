package com.ssafy.happynurse.domain.his.listener;

import com.ssafy.happynurse.domain.his.event.MedicationOrderCreatedEvent;
import com.ssafy.happynurse.domain.his.event.MedicationOrderUpdatedEvent;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicationOrderNotificationAdapter {

    private final NotificationDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreate(MedicationOrderCreatedEvent event) {
        if (event.getAssignedPractitionerId() == null) {
            log.info("의사 오더 알림 스킵: orderId={}, 담당 간호사 미배정",
                    event.getMedicationOrderId());
            return;
        }

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.order_change,
                event.getWardId(),
                event.getAssignedPractitionerId(),
                event.getPatientId(),
                event.getMedicationOrderId(),
                "새 의사 오더",
                event.getPatientName() + "님 - " + event.getOrderName(),
                event,
                event.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant(),
                null,
                PushPolicy.ASSIGN_DELIVERY
        );

        dispatcher.dispatch(envelope);
        log.info("[Notification] 신규 오더 알림 발사: orderId={}, nurseId={}",
                event.getMedicationOrderId(), event.getAssignedPractitionerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdate(MedicationOrderUpdatedEvent event) {
        if (event.getAssignedPractitionerId() == null) {
            log.info("의사 오더 변경 알림 스킵: orderId={}, 담당 간호사 미배정",
                    event.getMedicationOrderId());
            return;
        }

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.order_change,
                event.getWardId(),
                event.getAssignedPractitionerId(),
                event.getPatientId(),
                event.getMedicationOrderId(),
                "의사 오더 변경",
                event.getPatientName() + "님 - " + event.getOrderName(),
                event,
                event.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant(),
                null,
                PushPolicy.ASSIGN_DELIVERY
        );

        dispatcher.dispatch(envelope);
        log.info("[Notification] 오더 변경 알림 발사: orderId={}, nurseId={}",
                event.getMedicationOrderId(), event.getAssignedPractitionerId());
    }
}