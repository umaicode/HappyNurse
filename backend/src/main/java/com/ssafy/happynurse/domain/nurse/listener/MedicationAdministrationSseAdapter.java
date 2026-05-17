package com.ssafy.happynurse.domain.nurse.listener;

import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import com.ssafy.happynurse.domain.nurse.service.MedicationAdministrationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicationAdministrationSseAdapter {

    private final MedicationAdministrationSseService medicationAdministrationSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MedicationAdministrationSavedEvent event) {
        medicationAdministrationSseService.send(
                event.getTaggingId(),
                event.getPatientId(),
                event.getAuthorPractitionerId()
        );
    }
}