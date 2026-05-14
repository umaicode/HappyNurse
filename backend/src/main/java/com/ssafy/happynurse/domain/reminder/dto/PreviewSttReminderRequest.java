package com.ssafy.happynurse.domain.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "STT 발화 텍스트로부터 fireAt 만 미리 파싱 (저장 X)")
public record PreviewSttReminderRequest(
        @Schema(description = "STT 원문 발화 텍스트", example = "8시 30분에 혈압 체크")
        @NotBlank
        @Size(max = 1000)
        String sttText
) {
}
