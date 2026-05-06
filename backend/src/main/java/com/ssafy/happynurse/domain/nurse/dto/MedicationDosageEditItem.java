package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "투약 그룹 내 약별 1회 투여량 수정 항목")
public record MedicationDosageEditItem(
        @Schema(description = "투약 기록 PK", example = "6")
        @NotNull
        Long medicationAdminId,

        @Schema(description = "수정할 1회 투여량", example = "100")
        @NotNull
        BigDecimal dosageQuantity
) {
}
