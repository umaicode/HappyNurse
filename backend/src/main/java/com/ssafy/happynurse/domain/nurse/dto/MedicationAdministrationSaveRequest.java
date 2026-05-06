package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MedicationAdministrationSaveRequest(
        @Schema(description = "환자 PK", example = "1")
        @NotNull
        Long patientId,

        @Schema(description = "입원(Encounter) PK", example = "1")
        @NotNull
        Long encounterId,

        @Schema(description = "검증 완료된 처방 오더 PK 목록", example = "[12345, 12346]")
        @NotEmpty
        List<Long> medicationOrderIds
) {
}
