package com.ssafy.happynurse.domain.watch.scheduler;

import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
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
 * DB 폴링 → 인메모리 정밀 스케줄러 등록
 * (1) 주기
 * - 폴링 주기 : 1분
 * - lookahead : 지금 ~ +2분 fire 후보
 * (2) 동시 실행 방지
 * - ShedLock 적용 (다중 인스턴스에서 1서버에서만 폴링)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IvAlertPollingJob {

    static final Duration LOOKAHEAD = Duration.ofMinutes(2);
    private static final long FIVE_MIN_OFFSET_MINUTES = 5;

    private final IvInfusionRepository repository;
    private final IvAlertScheduler scheduler;
    private final IvAlertTimers timers;

    /**
     * 폴링 주기 1분, lockAtMostFor 5분, lockAtLeastFor 30초
     */
    @Scheduled(fixedDelay = 60_000L)
    @SchedulerLock(name = "iv-alert-polling", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        long startedNanos = System.nanoTime();

        int registered = registerWindow(now, now.plus(LOOKAHEAD));

        Duration duration = Duration.ofNanos(System.nanoTime() - startedNanos);
        timers.recordPollingDuration(duration);
        if (registered > 0) {
            log.info("IV alert polling: registered={}, duration_ms={}", registered, duration.toMillis());
        } else {
            log.debug("IV alert polling: no candidates, duration_ms={}", duration.toMillis());
        }
    }

    /**
     * 윈도우를 백워드 LOOKAHEAD 만큼 확장해 직전 다운타임 동안 놓친 알림 픽업
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        LocalDateTime now = LocalDateTime.now();
        int registered = registerWindow(now.minus(LOOKAHEAD), now.plus(LOOKAHEAD));
        log.info("IV alert warmup at startup: registered={}", registered);
    }

    /**
     * [from, to) 윈도우 안에 fire 시각이 들어오는 IV 들을 조회해 인메모리 스케줄러에 등록
     */
    private int registerWindow(LocalDateTime from, LocalDateTime to) {
        LocalDateTime fiveMinFrom = from.plusMinutes(FIVE_MIN_OFFSET_MINUTES);
        LocalDateTime fiveMinTo   = to.plusMinutes(FIVE_MIN_OFFSET_MINUTES);
        List<IvInfusion> fiveMinCandidates = repository.findDueFiveMinAlerts(fiveMinFrom, fiveMinTo);

        int registered = 0;
        for (IvInfusion iv : fiveMinCandidates) {
            if (!scheduler.isRegistered(iv.getIvInfusionId(), AlertType.FIVE_MIN_BEFORE)) {
                scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);
                registered++;
            }
        }

        List<IvInfusion> endCandidates = repository.findDueEndAlerts(from, to);
        for (IvInfusion iv : endCandidates) {
            if (!scheduler.isRegistered(iv.getIvInfusionId(), AlertType.COMPLETED)) {
                scheduler.register(iv, AlertType.COMPLETED);
                registered++;
            }
        }

        return registered;
    }
}
