package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "간호 기록 수동 작성 요청 (음성 없이 직접 입력하는 경우)")
public record NursingRecordManualCreateRequest(
        @Schema(description = "입원 PK", example = "42")
        Long encounterId,

        @Schema(description = "간호 기록 본문", example = "환자 통증 호소 NRS 4점, 진통제 투여 후 호전")
        String content
) {
}