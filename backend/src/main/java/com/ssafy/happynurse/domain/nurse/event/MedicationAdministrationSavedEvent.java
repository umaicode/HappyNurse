package com.ssafy.happynurse.domain.nurse.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MedicationAdministrationSavedEvent {
    private final String taggingId;
    private final Long encounterId;
    private final Long patientId;
    private final Long authorPractitionerId;
}