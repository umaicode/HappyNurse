package com.ssafy.happynurse.domain.webapp.listener;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationDispatcher;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.util.Optional;

/**
 * 환자 자가보고(SymptomSubmittedEvent)를 알림 envelope으로 변환하여 dispatcher로 전달.
 *
 * 모듈 오너십: webapp 도메인의 producer 어댑터 (notification 인프라 측이 아니라 webapp 안에 위치).
 * 의존성: notification.api 의 3개 (interface + DTO + enum) + EncounterRepository (wardId 조회).
 *
 * 도메인 규약: wardId 는 encounter (활성 입원) 가 진실원이므로 EncounterRepository 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SymptomSubmittedNotificationAdapter {

    private final NotificationDispatcher dispatcher;
    private final EncounterRepository encounterRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SymptomSubmittedEvent event) {
        if (event.getAssignedPractitionerId() == null) {
            log.info("증상 알림 스킵: patientId={}, 담당 간호사 미배정", event.getPatientId());
            return;
        }

        Optional<Long> wardIdOpt = encounterRepository.findCurrentWardIdByPatientId(event.getPatientId());
        if (wardIdOpt.isEmpty()) {
            log.warn("증상 알림 라우팅 실패 — patientId={} 의 활성 입원이 없음, dispatch 스킵",
                    event.getPatientId());
            return;
        }

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.self_report,
                wardIdOpt.get(),
                event.getAssignedPractitionerId(),
                event.getPatientId(),
                event.getSelfReportId(),
                event.getPatientName() + "님의 증상 알림",
                event.getSymptomText(),
                event,
                event.getSubmittedAt().atZone(ZoneId.systemDefault()).toInstant(),
                null,
                PushPolicy.ASSIGN_DELIVERY,
                null   // priority — Phase 5에서 event.getPriority()로 채움
        );

        dispatcher.dispatch(envelope);
    }
}