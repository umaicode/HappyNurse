package com.ssafy.happynurse.domain.nurse.listener;

import com.ssafy.happynurse.domain.nurse.dto.SseNotificationPayload;
import com.ssafy.happynurse.domain.nurse.service.SseEmitterManager;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SseNotificationListener {

    private final SseEmitterManager sseEmitterManager;

    // 증상 제출 트랜잭션이 커밋된 후 SSE를 전송
    // AFTER_COMMIT: 증상이 DB에 확실히 저장된 뒤에 알림을 보내기 위함.
    // -> SSE 전송이 실패해도 증상 저장은 영향을 받지 않게 된다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSymptomSubmitted(SymptomSubmittedEvent event) {
        if (event.getAssignedPractitionerId() == null) {
            // 담당 간호사가 없으면 SSE 전송 스킵
            return;
        }
        sseEmitterManager.sendTo(
                event.getAssignedPractitionerId(),
                new SseNotificationPayload(
                        event.getPatientName(),
                        event.getRoomName(),
                        event.getSymptomText(),
                        event.getSelfReportId(),
                        event.getSubmittedAt().toString()
                )
        );
    }
}
