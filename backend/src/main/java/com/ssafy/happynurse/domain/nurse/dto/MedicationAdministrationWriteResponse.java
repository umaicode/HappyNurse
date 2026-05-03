package com.ssafy.happynurse.domain.nurse.dto;

import com.ssafy.happynurse.domain.nurse.entity.RecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "투약 그룹 확정/수정 응답 (taggingId 단위)")
public record MedicationAdministrationWriteResponse(
        @Schema(description = "NFC 태깅 묶음 ID", example = "8b2a3f6c-7d4e-4a1b-9c2d-1e2f3a4b5c6d")
        String taggingId,

        @Schema(description = "그룹 상태", example = "confirmed")
        RecordStatus status,

        @Schema(description = "투약 시각", example = "2026-05-03T23:30:00")
        LocalDateTime effectiveDatetime,

        @Schema(description = "그룹 내 약 목록")
        List<MedicationItemResponse> medications
) {
}
