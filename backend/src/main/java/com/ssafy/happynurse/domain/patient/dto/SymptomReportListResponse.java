package com.ssafy.happynurse.domain.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SymptomReportListResponse(
        @Schema(description = "환자 PK", example = "1")
        Long patientId,

        @Schema(description = "환자 이름", example = "이석재")
        String patientName,

        @Schema(description = "총 증상 보고 수", example = "2")
        int totalCount,

        @Schema(description = "증상 보고 목록")
        List<SymptomReportItemResponse> symptoms
) {
}