package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "통합 노트 — 투약 기록(MEDICATION) 수정 요청 (medications/confirmedAt 모두 옵셔널, 둘 다 비어있으면 400)")
public record NursingNoteMedicationEditRequest(
        @Schema(description = "그룹 내 약별 1회 투여량 변경 항목 (그룹 내 medicationAdminId만 허용)")
        @Valid
        List<MedicationDosageEditItem> medications,

        @Schema(description = "그룹 일괄 적용 기록 시각 (effectiveDatetime). 같은 taggingId 의 모든 row 가 함께 갱신된다.",
                example = "2026-04-27T10:00:00")
        LocalDateTime confirmedAt
) {
}
