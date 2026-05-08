package com.ssafy.happynurse.domain.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "STT 음성 메모 알람 등록 요청 — 환자 관련/일반 업무 모두 동일 형태")
public record CreateSttReminderRequest(
        @Schema(description = "STT 원문 발화 텍스트. 알람 본문은 백엔드가 시간 표현을 제거해 자동 생성.",
                example = "2시간 뒤 인수인계 준비")
        @NotBlank
        @Size(max = 1000)
        String sttText,

        @Schema(description = "알람 시각 (epoch millis). /preview 응답값 또는 사용자가 워치에서 수정한 값을 그대로 전달.",
                example = "1810400400000")
        @NotNull
        Long fireAtEpochMillis
) {
}
