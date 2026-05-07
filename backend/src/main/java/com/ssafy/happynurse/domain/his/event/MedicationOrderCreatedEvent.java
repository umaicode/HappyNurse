package com.ssafy.happynurse.domain.his.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class MedicationOrderCreatedEvent {

    private final Long medicationOrderId;
    private final Long patientId;
    private final String patientName;
    private final Long assignedPractitionerId;
    private final Long wardId;
    private final String orderName;
    private final LocalDateTime occurredAt;
}