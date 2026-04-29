package com.ssafy.happynurse.domain.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WardListResponse(
        @Schema(description = "병동 PK", example = "3")
        Long wardId,

        @Schema(description = "병동명", example = "내과 3병동")
        String wardName
) {
}