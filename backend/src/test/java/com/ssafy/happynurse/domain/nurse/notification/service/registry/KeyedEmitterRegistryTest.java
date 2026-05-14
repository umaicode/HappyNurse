package com.ssafy.happynurse.domain.nurse.notification.service.registry;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class KeyedEmitterRegistryTest {

    /** 테스트용 구체 서브클래스 (KeyedEmitterRegistry는 abstract). */
    static class TestRegistry extends KeyedEmitterRegistry { }

    private TestRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TestRegistry();
    }

    @Test
    void register_returnsEmitterAndStoresUnderKey() {
        SseEmitter emitter = registry.register(7L);
        assertThat(emitter).isNotNull();
        assertThat(registry.connectionCount(7L)).isEqualTo(1);
    }

    @Test
    void register_multipleEmittersUnderSameKey_allStored() {
        registry.register(7L);
        registry.register(7L);
        registry.register(7L);
        assertThat(registry.connectionCount(7L)).isEqualTo(3);
    }

    @Test
    void register_differentKeys_isolated() {
        registry.register(1L);
        registry.register(2L);
        assertThat(registry.connectionCount(1L)).isEqualTo(1);
        assertThat(registry.connectionCount(2L)).isEqualTo(1);
    }

    @Test
    void send_unknownKey_isNoOp() {
        registry.send(999L, sampleEnvelope());
        // 예외 없이 끝나면 통과
    }

    @Test
    void connectionCount_emptyKey_returnsZero() {
        assertThat(registry.connectionCount(42L)).isEqualTo(0);
    }

    private NotificationEnvelope sampleEnvelope() {
        return new NotificationEnvelope(
                SourceType.self_report,
                1L, 7L, 100L, 50L,
                "title", "body",
                "payload", Instant.now(), null,
                PushPolicy.ASSIGN_DELIVERY, null);
    }
}