package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "투약 그룹 내 약별 용량 변경 항목")
public record MedicationDosageUpdateRequest(
        @Schema(description = "투약 기록 PK", example = "31")
        @NotNull
        Long medicationAdminId,

        @Schema(description = "수정할 1회 투여량", example = "1.500")
        BigDecimal dosageQuantity,

        @Schema(description = "수정할 투약 단위", example = "tab")
        String dosageUnit
) {
}
