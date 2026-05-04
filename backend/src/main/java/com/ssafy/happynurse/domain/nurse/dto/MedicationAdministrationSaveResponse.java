package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MedicationAdministrationSaveResponse(
        @Schema(description = "한 번의 NFC 태깅 세션을 식별하는 그룹 ID (UUID)",
                example = "8f3c8b1e-9b1d-4f8c-a8d3-2b7c1e6e4a90")
        String taggingId,

        @Schema(description = "저장된 투약 기록 수", example = "3")
        int savedCount,

        @Schema(description = "저장된 투약 기록 PK 목록", example = "[101, 102, 103]")
        List<Long> medicationAdminIds
) {
}
