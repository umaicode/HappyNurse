package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "증상 제출 응답")
@Getter
@AllArgsConstructor
public class SymptomSubmitResponse {

    @Schema(description = "자가보고 ID", example = "42")
    private Long selfReportId;

    @Schema(description = "제출 시각")
    private LocalDateTime submittedAt;
}
