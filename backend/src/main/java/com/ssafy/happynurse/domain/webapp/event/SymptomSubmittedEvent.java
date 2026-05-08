package com.ssafy.happynurse.domain.webapp.event;

import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
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
    private SymptomPriority priority;       // 분류 결과. null 가능 (분류 실패 등)
}
