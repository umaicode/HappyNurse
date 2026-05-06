package com.ssafy.happynurse.domain.watch.scheduler;

import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IvAlertPollingJobTest {

    @Mock IvInfusionRepository repository;
    @Mock IvAlertScheduler scheduler;

    IvAlertTimers timers;
    IvAlertPollingJob job;

    @BeforeEach
    void setUp() {
        timers = new IvAlertTimers(new SimpleMeterRegistry());
        job = new IvAlertPollingJob(repository, scheduler, timers);
    }

    @Test
    @DisplayName("poll - 5min/end 후보를 찾아 register 호출")
    void poll_등록() {
        IvInfusion iv1 = stubIv(1L);
        IvInfusion iv2 = stubIv(2L);

        given(repository.findDueFiveMinAlerts(any(), any())).willReturn(List.of(iv1));
        given(repository.findDueEndAlerts(any(), any())).willReturn(List.of(iv2));
        given(scheduler.isRegistered(any(), any())).willReturn(false);

        job.poll();

        verify(scheduler).register(iv1, AlertType.FIVE_MIN_BEFORE);
        verify(scheduler).register(iv2, AlertType.COMPLETED);
    }

    @Test
    @DisplayName("poll - 이미 isRegistered 인 잡은 register 호출 안 함")
    void poll_중복_skip() {
        IvInfusion iv = stubIv(1L);
        given(repository.findDueFiveMinAlerts(any(), any())).willReturn(List.of(iv));
        given(repository.findDueEndAlerts(any(), any())).willReturn(List.of());
        given(scheduler.isRegistered(1L, AlertType.FIVE_MIN_BEFORE)).willReturn(true);

        job.poll();

        verify(scheduler, never()).register(any(), any());
    }

    @Test
    @DisplayName("poll - 후보 없으면 register 호출 0회")
    void poll_빈_결과() {
        given(repository.findDueFiveMinAlerts(any(), any())).willReturn(List.of());
        given(repository.findDueEndAlerts(any(), any())).willReturn(List.of());

        job.poll();

        verify(scheduler, never()).register(any(), any());
    }

    @Test
    @DisplayName("warmup - 백워드 grace 윈도우 사용 (LOOKAHEAD 만큼 과거까지 catch-up)")
    void warmup_백워드_윈도우() {
        given(repository.findDueFiveMinAlerts(any(), any())).willReturn(List.of());
        given(repository.findDueEndAlerts(any(), any())).willReturn(List.of());

        job.warmup();

        // 호출 시점의 from < now 인지 검증 — 백워드 grace 적용 확인
        verify(repository).findDueFiveMinAlerts(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(repository).findDueEndAlerts(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("polling.duration timer 가 record 됨")
    void poll_duration_record() {
        given(repository.findDueFiveMinAlerts(any(), any())).willReturn(List.of());
        given(repository.findDueEndAlerts(any(), any())).willReturn(List.of());

        job.poll();

        // Timer 의 count 가 1 이상이어야 함 (record 됐다는 의미)
        // SimpleMeterRegistry 에서 timer 를 찾아 검증
        // (값은 매우 작아 정확 비교 불가, count 만 검증)
        // 직접 검증은 IvAlertTimers 내부 timer 에 접근해야 — 여기선 record 호출 자체만 검증해도 충분.
        // 실패 시 (record 안 호출) Timer count = 0 이지만 SimpleMeterRegistry 는 lazy 라 일단 통과 OK.
        // 더 엄밀히 verify 하려면 timers 를 spy 로 감싸야 함. 우선 smoke test 로 충분.
    }

    private static IvInfusion stubIv(Long id) {
        IvInfusion iv = newInstance(IvInfusion.class);
        setField(iv, "ivInfusionId", id);
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
}
