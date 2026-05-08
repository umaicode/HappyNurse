package com.ssafy.happynurse.domain.reminder.scheduler;

import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.reminder.repository.SttReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DB 폴링 → 인메모리 정밀 스케줄러 등록 (IvAlertPollingJob 패턴 클론).
 * - 폴링 주기 1분, lookahead 2분
 * - ShedLock 으로 다중 인스턴스 동시 폴링 방지
 * - ApplicationReady 시 backward 윈도우까지 확장해 다운타임 동안 놓친 알림 픽업
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttReminderPollingJob {

    static final Duration LOOKAHEAD = Duration.ofMinutes(2);

    private final SttReminderRepository repository;
    private final SttReminderScheduler scheduler;

    @Scheduled(fixedDelay = 60_000L)
    @SchedulerLock(name = "stt-reminder-polling", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        int registered = registerWindow(now, now.plus(LOOKAHEAD));
        if (registered > 0) {
            log.info("STT reminder polling: registered={}", registered);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        LocalDateTime now = LocalDateTime.now();
        int registered = registerWindow(now.minus(LOOKAHEAD), now.plus(LOOKAHEAD));
        log.info("STT reminder warmup at startup: registered={}", registered);
    }

    private int registerWindow(LocalDateTime from, LocalDateTime to) {
        List<SttReminder> candidates = repository.findDueAlerts(from, to);
        int registered = 0;
        for (SttReminder r : candidates) {
            if (!scheduler.isRegistered(r.getSttReminderId())) {
                scheduler.register(r);
                registered++;
            }
        }
        return registered;
    }
}
