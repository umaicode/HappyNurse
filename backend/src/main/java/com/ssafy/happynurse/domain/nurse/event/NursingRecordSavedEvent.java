package com.ssafy.happynurse.domain.nurse.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NursingRecordSavedEvent {
    private final Long nursingRecordId;
    private final Long patientId;
    private final Long authorPractitionerId;
}