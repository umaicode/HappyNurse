package com.ssafy.happynurse.domain.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "STT 발화 시간 파싱 결과")
public record PreviewSttReminderResponse(
        @Schema(description = "파싱된 알람 시각 (epoch millis, KST)") long fireAtEpochMillis
) {
}
