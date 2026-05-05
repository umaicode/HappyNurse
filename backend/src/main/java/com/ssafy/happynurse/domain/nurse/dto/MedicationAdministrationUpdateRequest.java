package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "투약 그룹(taggingId) 수정 요청 (모든 필드 옵셔널)")
public record MedicationAdministrationUpdateRequest(
        @Schema(description = "그룹 일괄 적용 투약 시각", example = "2026-05-03T23:30:00")
        LocalDateTime effectiveDatetime,

        @Schema(description = "약별 용량 변경 목록 (요청 시 그룹 내 medicationAdminId만 허용)")
        @Valid
        List<MedicationDosageUpdateRequest> medications
) {
}
