package com.ssafy.happynurse.domain.watch.scheduler;

import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.event.IvAlertEvent;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IvAlertSchedulerTest {

    @Mock IvInfusionRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PlatformTransactionManager txManager;

    private ThreadPoolTaskScheduler taskScheduler;
    private MeterRegistry meterRegistry;
    private IvAlertTimers timers;
    private IvAlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        taskScheduler.initialize();

        meterRegistry = new SimpleMeterRegistry();
        timers = new IvAlertTimers(meterRegistry);

        // TransactionTemplate.execute 가 callback 을 즉시 실행하도록 stub.
        // lenient — 일부 테스트(register/cancel/gauge)는 fire 를 안 타서 이 stub 미사용 → strict 모드 회피
        lenient().when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        scheduler = new IvAlertScheduler(taskScheduler, repository, eventPublisher,
                txManager, timers, meterRegistry);
        scheduler.registerSizeGauge();
    }

    // ---------- register / cancel / size / isRegistered ----------

    @Test
    @DisplayName("register - jobs Map 에 추가, isRegistered=true, size=1")
    void register_정상() {
        IvInfusion iv = stubIv(1L, LocalDateTime.now().plusMinutes(10));

        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);

        assertThat(scheduler.isRegistered(1L, AlertType.FIVE_MIN_BEFORE)).isTrue();
        assertThat(scheduler.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("register - 같은 (id, alertType) 두 번 호출 시 첫 future 가 cancel 되고 두 번째로 교체")
    void register_중복_교체() {
        IvInfusion iv = stubIv(1L, LocalDateTime.now().plusMinutes(10));

        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);
        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);

        // size 는 그대로 1, 두 번째 future 가 활성
        assertThat(scheduler.size()).isEqualTo(1);
        assertThat(scheduler.isRegistered(1L, AlertType.FIVE_MIN_BEFORE)).isTrue();
    }

    @Test
    @DisplayName("cancel - 잡 제거, isRegistered=false")
    void cancel_정상() {
        IvInfusion iv = stubIv(1L, LocalDateTime.now().plusMinutes(10));
        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);

        scheduler.cancel(1L, AlertType.FIVE_MIN_BEFORE);

        assertThat(scheduler.isRegistered(1L, AlertType.FIVE_MIN_BEFORE)).isFalse();
        assertThat(scheduler.size()).isZero();
    }

    @Test
    @DisplayName("cancelAll - 양쪽 alertType 모두 제거")
    void cancelAll_정상() {
        IvInfusion iv = stubIv(1L, LocalDateTime.now().plusMinutes(10));
        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);
        scheduler.register(iv, AlertType.COMPLETED);

        scheduler.cancelAll(1L);

        assertThat(scheduler.size()).isZero();
    }

    // ---------- fire (CAS + event publish) ----------

    @Test
    @DisplayName("fire - CAS 영향 1 → IvAlertEvent publish, jobs 에서 제거")
    void fire_CAS_성공_publish() {
        // 과거 시각으로 schedule → TaskScheduler 가 즉시 실행
        IvInfusion iv = stubIv(1L, LocalDateTime.now().minusSeconds(1));
        given(repository.markFiveMinAlertSentIfNotSent(eq(1L), any())).willReturn(1);

        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<IvAlertEvent> captor = ArgumentCaptor.forClass(IvAlertEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            IvAlertEvent published = captor.getValue();
            assertThat(published.ivInfusionId()).isEqualTo(1L);
            assertThat(published.alertType()).isEqualTo(AlertType.FIVE_MIN_BEFORE);
            assertThat(scheduler.size()).isZero(); // jobs 에서 제거됨
        });
    }

    @Test
    @DisplayName("fire - CAS 영향 0 (이미 발사됨/상태 변경) → publishEvent 호출 안 됨")
    void fire_CAS_실패_skip() {
        IvInfusion iv = stubIv(2L, LocalDateTime.now().minusSeconds(1));
        given(repository.markFiveMinAlertSentIfNotSent(eq(2L), any())).willReturn(0);

        scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(repository).markFiveMinAlertSentIfNotSent(eq(2L), any());
            verify(eventPublisher, never()).publishEvent(any(IvAlertEvent.class));
            assertThat(scheduler.size()).isZero();
        });
    }

    // ---------- 메트릭 ----------

    @Test
    @DisplayName("Gauge iv.alert.scheduled.size 가 jobs 사이즈를 반영")
    void gauge_size_반영() {
        IvInfusion iv1 = stubIv(1L, LocalDateTime.now().plusMinutes(10));
        IvInfusion iv2 = stubIv(2L, LocalDateTime.now().plusMinutes(15));

        scheduler.register(iv1, AlertType.FIVE_MIN_BEFORE);
        scheduler.register(iv2, AlertType.COMPLETED);

        Double gaugeValue = meterRegistry.find("iv.alert.scheduled.size").gauge().value();
        assertThat(gaugeValue).isEqualTo(2.0);
    }

    // ---------- helpers ----------

    private static IvInfusion stubIv(Long id, LocalDateTime expectedEndAt) {
        IvInfusion iv = newInstance(IvInfusion.class);
        setField(iv, "ivInfusionId", id);
        setField(iv, "expectedEndAt", expectedEndAt);
        return iv;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object obj, String name, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                var f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException ignore) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("no such field: " + name);
    }

    /** TransactionTemplate 가 callback 을 즉시 실행하도록 stub. */
    @SuppressWarnings("unused")
    private TransactionStatus stubTxBehavior(TransactionCallback<?> cb) {
        return new SimpleTransactionStatus();
    }
}
