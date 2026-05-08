package com.ssafy.happynurse.domain.reminder.dto;

import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZoneId;

@Schema(description = "STT 음성 메모 알람 등록 응답")
public record SttReminderResponse(
        @Schema(description = "알람 ID") Long sttReminderId,
        @Schema(description = "발사 예정 시각 (epoch milliseconds, KST → UTC)") long fireAtEpochMillis,
        @Schema(description = "알람 본문") String contentSummary
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static SttReminderResponse of(SttReminder r) {
        long millis = r.getFireAt().atZone(KST).toInstant().toEpochMilli();
        return new SttReminderResponse(r.getSttReminderId(), millis, r.getContentSummary());
    }
}
