package com.ssafy.happynurse.domain.watch.scheduler;

import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.event.IvAlertEvent;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 인메모리 정밀 스케줄러
 * 1. DB 폴링이 lookahead 윈도우 내 row 를 발견하면 이 스케줄러에 register
 * 2. 발사 시각이 되면 fire 가 실행돼 DB CAS 마킹 + 도메인 이벤트 publish
 */
@Slf4j
@Component
public class IvAlertScheduler {

    private final TaskScheduler taskScheduler;
    private final IvInfusionRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate txTemplate;
    private final IvAlertTimers timers;
    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<DispatchKey, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();

    public IvAlertScheduler(
            @Qualifier("ivAlertTaskScheduler") TaskScheduler taskScheduler,
            IvInfusionRepository repository,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager txManager,
            IvAlertTimers timers,
            MeterRegistry meterRegistry
    ) {
        this.taskScheduler = taskScheduler;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.txTemplate = new TransactionTemplate(txManager);
        this.timers = timers;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerSizeGauge() {
        // 인메모리 Map 사이즈 게이지 : (Timers → Scheduler) 의존 회피 위해 여기서 등록
        Gauge.builder("iv.alert.scheduled.size", this, IvAlertScheduler::size)
                .description("Number of IV alert jobs currently held in memory")
                .register(meterRegistry);
    }

    /** (ivInfusionId, alertType) 잡 등록 이미 있으면 cancel + 교체 (속도 변경 대응) */
    public void register(IvInfusion iv, AlertType alertType) {
        DispatchKey key = new DispatchKey(iv.getIvInfusionId(), alertType);
        Instant fireInstant = computeFireInstant(iv, alertType);

        jobs.compute(key, (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                existing.cancel(false);
            }
            return taskScheduler.schedule(() -> fire(k, fireInstant), fireInstant);
        });
        log.debug("IV alert scheduled: infusionId={}, alertType={}, fireAt={}",
                iv.getIvInfusionId(), alertType, fireInstant);
    }

    public void cancel(Long ivInfusionId, AlertType alertType) {
        DispatchKey key = new DispatchKey(ivInfusionId, alertType);
        ScheduledFuture<?> future = jobs.remove(key);
        if (future != null) {
            future.cancel(false);
            log.debug("IV alert cancelled: infusionId={}, alertType={}", ivInfusionId, alertType);
        }
    }

    public void cancelAll(Long ivInfusionId) {
        for (AlertType type : AlertType.values()) {
            cancel(ivInfusionId, type);
        }
    }

    public boolean isRegistered(Long ivInfusionId, AlertType alertType) {
        ScheduledFuture<?> f = jobs.get(new DispatchKey(ivInfusionId, alertType));
        return f != null && !f.isDone();
    }

    public int size() {
        return jobs.size();
    }

    /**
     * 발사 — TaskScheduler 가 fire time 에 호출
     */
    private void fire(DispatchKey key, Instant expectedFireAt) {
        Instant actualFireAt = Instant.now();
        timers.recordDispatchDelay(Duration.between(expectedFireAt, actualFireAt));

        try {
            Boolean fired = txTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now();
                int affected = switch (key.alertType()) {
                    case FIVE_MIN_BEFORE -> repository.markFiveMinAlertSentIfNotSent(key.ivInfusionId(), now);
                    case COMPLETED      -> repository.markEndAlertSentIfNotSent(key.ivInfusionId(), now);
                };
                if (affected == 1) {
                    eventPublisher.publishEvent(new IvAlertEvent(
                            key.ivInfusionId(), key.alertType(), actualFireAt));
                    return true;
                }
                return false;
            });

            if (Boolean.TRUE.equals(fired)) {
                log.info("IV alert fired: infusionId={}, alertType={}, schedule_delay_ms={}",
                        key.ivInfusionId(), key.alertType(),
                        Duration.between(expectedFireAt, actualFireAt).toMillis());
            } else {
                log.debug("IV alert skipped (already sent or not IN_PROGRESS): infusionId={}, alertType={}",
                        key.ivInfusionId(), key.alertType());
            }
        } catch (Exception e) {
            log.error("IV alert fire failed (will be retried by polling): infusionId={}, alertType={}",
                    key.ivInfusionId(), key.alertType(), e);
        } finally {
            jobs.remove(key);
        }
    }

    private static Instant computeFireInstant(IvInfusion iv, AlertType alertType) {
        LocalDateTime fireAt = switch (alertType) {
            case FIVE_MIN_BEFORE -> iv.getExpectedEndAt().minusMinutes(5);
            case COMPLETED      -> iv.getExpectedEndAt();
        };
        return fireAt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
