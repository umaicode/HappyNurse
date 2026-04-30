package com.ssafy.happynurse.domain.patient.dto;

import com.ssafy.happynurse.domain.webapp.entity.InputMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SymptomReportItemResponse(
        @Schema(description = "증상 보고 PK", example = "1")
        Long selfReportId,

        @Schema(description = "입력 방식", example = "quick_button")
        InputMethod inputMethod,

        @Schema(description = "버튼 라벨 (버튼 선택 시)", example = "통증")
        String buttonLabel,

        @Schema(description = "증상 텍스트", example = "통증")
        String symptomText,

        @Schema(description = "제출 시간", example = "2026-04-29T10:30:00")
        LocalDateTime submittedAt
) {
}
