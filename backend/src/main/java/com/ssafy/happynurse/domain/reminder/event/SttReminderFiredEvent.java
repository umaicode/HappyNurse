package com.ssafy.happynurse.domain.reminder.event;

import java.time.Instant;

/**
 * 인메모리 스케줄러가 fire 시각에 CAS 마킹 성공 후 publish.
 * AFTER_COMMIT 으로 NotificationAdapter 가 받아 NotificationEnvelope 으로 변환.
 */
public record SttReminderFiredEvent(Long sttReminderId, Instant firedAt) {
}
