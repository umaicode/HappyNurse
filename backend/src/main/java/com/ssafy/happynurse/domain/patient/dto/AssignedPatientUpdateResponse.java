package com.ssafy.happynurse.domain.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AssignedPatientUpdateResponse(
        @Schema(description = "본인 담당이 된 입원 ID 목록", example = "[100, 101, 102]")
        List<Long> assignedEncounterIds,

        @Schema(description = "해제된 입원 ID 목록 (다른 사람이 가져간 건은 제외)", example = "[203]")
        List<Long> releasedEncounterIds,

        @Schema(description = "다른 간호사 담당이던 환자를 본인으로 가져온 수", example = "1")
        int overwroteFromOthersCount
) {
}
