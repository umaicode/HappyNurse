package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "간호 기록 수정 요청 (모든 필드 옵셔널 — 포함된 필드만 갱신)")
public record NursingRecordUpdateRequest(
        @Schema(description = "수정할 본문 (요청에 포함되면 비어있을 수 없음). draft면 editContent, 그 외엔 finalContent로 갱신되며 status는 amended로 전환된다.",
                example = "수정된 간호 기록 본문")
        String content,

        @Schema(description = "수정할 확정 시각", example = "2026-05-03T11:30:00")
        LocalDateTime confirmedAt
) {
}
