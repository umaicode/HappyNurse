package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NursingRecordFactory {

    public NursingRecord createManual(Long patientId,
                                      Long encounterId,
                                      Long authorPractitionerId,
                                      String content,
                                      LocalDateTime confirmedAt) {
        return NursingRecord.builder()
                .patientId(patientId)
                .encounterId(encounterId)
                .authorPractitionerId(authorPractitionerId)
                .status(RecordStatus.confirmed)
                .finalContent(content)
                .confirmedAt(confirmedAt != null ? confirmedAt : LocalDateTime.now())
                .build();
    }
}
