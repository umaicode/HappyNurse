package com.ssafy.happynurse.domain.watch.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * IV 알림 타이머 모음
 * Gauge ({@code iv.alert.scheduled.size}) 는 IvAlertScheduler 가 자기 size() 를 직접 등록
 * (Timers → Scheduler) 의존 (Scheduler → Timers)
 */
@Component
public class IvAlertTimers {

    private final Timer dispatchDelay;
    private final Timer pollingDuration;

    public IvAlertTimers(MeterRegistry registry) {
        this.dispatchDelay = Timer.builder("iv.alert.dispatch.delay")
                .description("Difference between scheduled fire instant and actual fire entry (TaskScheduler precision)")
                .register(registry);
        this.pollingDuration = Timer.builder("iv.alert.polling.duration")
                .description("Single IV alert polling cycle duration")
                .register(registry);
    }

    public void recordDispatchDelay(Duration delay) {
        // 음수(조기 발사) 방지 — 비정상 시계라도 Timer 가 negative 거부
        dispatchDelay.record(delay.isNegative() ? Duration.ZERO : delay);
    }

    public void recordPollingDuration(Duration duration) {
        pollingDuration.record(duration);
    }
}
