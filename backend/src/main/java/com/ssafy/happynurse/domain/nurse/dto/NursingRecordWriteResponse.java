package com.ssafy.happynurse.domain.nurse.dto;

import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "간호 기록 확정/수정 응답 (변경 후 핵심 필드)")
public record NursingRecordWriteResponse(
        @Schema(description = "간호 기록 PK", example = "12")
        Long nursingRecordId,

        @Schema(description = "변경 후 상태", example = "confirmed")
        RecordStatus status,

        @Schema(description = "현재 노출 본문 (status에 따라 editContent 또는 finalContent)",
                example = "환자 통증 호소 NRS 4점")
        String content,

        @Schema(description = "확정 시각", example = "2026-05-03T14:30:00")
        LocalDateTime confirmedAt
) {
}
