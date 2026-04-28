package com.ssafy.happynurse.domain.webapp.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SymptomSubmittedEvent {

    private Long assignedPractitionerId;    // SSE + FCM 공통 타겟
    private Long patientId;
    private String patientName;
    private String roomName;
    private String symptomText;
    private Long selfReportId;
    private LocalDateTime submittedAt;
}
