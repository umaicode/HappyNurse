package com.ssafy.happynurse.domain.reminder.dto;

import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZoneId;

@Schema(description = "STT 음성 메모 알람 리스트 항목")
public record SttReminderListItemResponse(
        @Schema(description = "알람 ID") Long sttReminderId,
        @Schema(description = "알람 본문") String contentSummary,
        @Schema(description = "알람 시각 (epoch millis, KST)") long fireAtEpochMillis,
        @Schema(description = "STT 원문 (워치 디테일 화면용)") String sttText
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static SttReminderListItemResponse of(SttReminder r) {
        long millis = r.getFireAt().atZone(KST).toInstant().toEpochMilli();
        return new SttReminderListItemResponse(
                r.getSttReminderId(),
                r.getContentSummary(),
                millis,
                r.getSttText()
        );
    }
}
