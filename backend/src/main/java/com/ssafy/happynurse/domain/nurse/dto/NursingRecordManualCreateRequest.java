package com.ssafy.happynurse.domain.nurse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "간호 기록 수동 작성 요청 (음성 없이 직접 입력하는 경우)")
public record NursingRecordManualCreateRequest(
        @Schema(description = "입원 PK", example = "42")
        Long encounterId,

        @Schema(description = "간호 기록 본문", example = "환자 통증 호소 NRS 4점, 진통제 투여 후 호전")
        String content,

        @Schema(description = "확정 시각 (옵셔널). 미지정 시 서버 현재 시각으로 저장. 기록 사이에 끼워넣을 때는 클라이언트가 prev/next 사이 값을 계산해 전달",
                example = "2026-05-03T14:37:00")
        LocalDateTime confirmedAt
) {
}
