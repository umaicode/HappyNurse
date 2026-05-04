package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MedicationVerifyResponse(
        @Schema(description = "검증 성공 여부", example = "true")
        boolean verified,

        @Schema(description = "검증된 처방 오더 PK", example = "12345")
        Long medicationOrderId
) {
    public static MedicationVerifyResponse success(Long medicationOrderId) {
        return new MedicationVerifyResponse(true, medicationOrderId);
    }
}
