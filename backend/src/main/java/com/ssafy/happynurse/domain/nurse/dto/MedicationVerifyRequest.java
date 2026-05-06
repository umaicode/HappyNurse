package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicationVerifyRequest(
        @Schema(description = "환자 PK (직전 wristband 태깅으로 확정된 값)", example = "1")
        @NotNull
        Long patientId,

        @Schema(description = "스캔한 약물 NFC 태그 UID (한 번에 하나)", example = "UID-DRUG-001")
        @NotBlank
        String tagUid
) {
}
