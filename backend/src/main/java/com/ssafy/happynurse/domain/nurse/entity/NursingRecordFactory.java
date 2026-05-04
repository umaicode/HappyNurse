package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NursingRecordFactory {

    public NursingRecord createManual(Patient patient,
                                      Encounter encounter,
                                      Practitioner authorPractitioner,
                                      String content) {
        return NursingRecord.builder()
                .patient(patient)
                .encounter(encounter)
                .authorPractitioner(authorPractitioner)
                .status(RecordStatus.confirmed)
                .version("")
                .finalContent(content)
                .confirmedAt(LocalDateTime.now())
                .build();
    }
}