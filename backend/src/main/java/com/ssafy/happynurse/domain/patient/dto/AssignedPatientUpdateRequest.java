package com.ssafy.happynurse.domain.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignedPatientUpdateRequest(
        @Schema(description = "내가 담당할 입원(Encounter) ID 목록 (체크된 항목 전체).",
                example = "[1, 2, 3]")
        @NotNull
        List<Long> encounterIds
) {
}
