package com.ssafy.happynurse.domain.reminder.listener;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.reminder.event.SttReminderFiredEvent;
import com.ssafy.happynurse.domain.reminder.repository.SttReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * SttReminderFiredEvent 수신 → NotificationEnvelope 변환 → dispatcher 위임.
 * 워치는 sourceType=timer 데이터의 patientName/contentSummary/roomBedTime 키로 풀스크린 알람 표시.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttReminderNotificationAdapter {

    private final SttReminderRepository repository;
    private final NotificationDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SttReminderFiredEvent event) {
        SttReminder reminder = repository.findByIdWithRoutingInfo(event.sttReminderId()).orElse(null);
        if (reminder == null) {
            log.warn("STT reminder dispatch skip: not found. id={}", event.sttReminderId());
            return;
        }

        Long recipientId = reminder.getPractitioner().getPractitionerId();

        Map<String, String> payload = new HashMap<>();
        payload.put("contentSummary", reminder.getContentSummary());

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.timer,
                reminder.getWardId(),
                recipientId,
                null, // 환자 무관 알람도 동일 형태로 처리
                reminder.getSttReminderId(),
                "음성 메모 알림",
                reminder.getContentSummary(),
                payload,
                event.firedAt(),
                null,
                PushPolicy.PERSONAL_INFO,
                null // priority는 환자 요청 알림 전용 — 타이머는 미사용
        );

        dispatcher.dispatch(envelope);
        log.info("STT reminder dispatched: id={}, recipientId={}", reminder.getSttReminderId(), recipientId);
    }
}
