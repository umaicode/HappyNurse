package com.ssafy.happynurse.domain.reminder.scheduler;

import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import com.ssafy.happynurse.domain.reminder.event.SttReminderFiredEvent;
import com.ssafy.happynurse.domain.reminder.repository.SttReminderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 인메모리 정밀 스케줄러 (IvAlertScheduler 패턴 클론, 단일 알림 타입이라 AlertType 분기 제거).
 * 1. 폴링이 lookahead 윈도우 내 row 발견 → register
 * 2. 발사 시각이 되면 fire — DB CAS 마킹 후 도메인 이벤트 publish
 */
@Slf4j
@Component
public class SttReminderScheduler {

    private final TaskScheduler taskScheduler;
    private final SttReminderRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate txTemplate;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();

    public SttReminderScheduler(
            @Qualifier("sttReminderTaskScheduler") TaskScheduler taskScheduler,
            SttReminderRepository repository,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager txManager
    ) {
        this.taskScheduler = taskScheduler;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /** 잡 등록. 이미 있으면 cancel + 교체 (재등록 대응). */
    public void register(SttReminder reminder) {
        Long id = reminder.getSttReminderId();
        Instant fireInstant = reminder.getFireAt().atZone(ZoneId.systemDefault()).toInstant();

        jobs.compute(id, (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                existing.cancel(false);
            }
            return taskScheduler.schedule(() -> fire(id, fireInstant), fireInstant);
        });
        log.debug("STT reminder scheduled: id={}, fireAt={}", id, fireInstant);
    }

    public void cancel(Long reminderId) {
        ScheduledFuture<?> future = jobs.remove(reminderId);
        if (future != null) {
            future.cancel(false);
            log.debug("STT reminder cancelled in-memory: id={}", reminderId);
        }
    }

    public boolean isRegistered(Long reminderId) {
        ScheduledFuture<?> f = jobs.get(reminderId);
        return f != null && !f.isDone();
    }

    public int size() {
        return jobs.size();
    }

    /** TaskScheduler 가 fire time 에 호출. */
    private void fire(Long reminderId, Instant expectedFireAt) {
        Instant actualFireAt = Instant.now();
        try {
            Boolean fired = txTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now();
                int affected = repository.markAlertSentIfNotSent(reminderId, now);
                if (affected == 1) {
                    eventPublisher.publishEvent(new SttReminderFiredEvent(reminderId, actualFireAt));
                    return true;
                }
                return false;
            });

            if (Boolean.TRUE.equals(fired)) {
                log.info("STT reminder fired: id={}, schedule_delay_ms={}",
                        reminderId, java.time.Duration.between(expectedFireAt, actualFireAt).toMillis());
            } else {
                log.debug("STT reminder skipped (already sent or canceled): id={}", reminderId);
            }
        } catch (Exception e) {
            log.error("STT reminder fire failed (will be retried by polling): id={}", reminderId, e);
        } finally {
            jobs.remove(reminderId);
        }
    }
}
