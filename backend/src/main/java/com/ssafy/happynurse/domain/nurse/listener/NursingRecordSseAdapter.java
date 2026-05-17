package com.ssafy.happynurse.domain.nurse.listener;

import com.ssafy.happynurse.domain.nurse.event.NursingRecordSavedEvent;
import com.ssafy.happynurse.domain.nurse.service.NursingRecordSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NursingRecordSseAdapter {

    private final NursingRecordSseService nursingRecordSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NursingRecordSavedEvent event) {
        nursingRecordSseService.send(event.getNursingRecordId());
    }
}