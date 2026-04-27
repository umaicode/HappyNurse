package com.ssafy.happynurse.domain.nurse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SseNotificationPayload {

    private String patientName;
    private String roomName;
    private String symptomText;
    private Long selfReportId;
    private LocalDateTime submittedAt;
}
