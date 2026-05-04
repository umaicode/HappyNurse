package com.ssafy.happynurse.domain.doctor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MedicationOrderListResponse(
        @Schema(description = "입원 PK", example = "42")
        Long encounterId,

        @Schema(description = "환자 PK", example = "3")
        Long patientId,

        @Schema(description = "환자 이름", example = "이승연")
        String patientName,

        @Schema(description = "총 오더 수", example = "4")
        int totalCount,

        @Schema(description = "오더 목록")
        List<MedicationOrderItemResponse> orders
) {
}