package com.ssafy.happynurse.domain.nurse.notification.service.registry;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Long 키(예: wardId, practitionerId)로 SseEmitter들을 보관하고
 * 같은 키 아래의 모든 emitter에 envelope을 fan-out한다.
 *
 * 같은 키에 여러 연결(예: 같은 병동의 PC 여러 대 / 같은 간호사의 여러 디바이스)이
 * 동시에 붙을 수 있어, 키 밑은 connId(UUID) 기준 inner Map으로 관리한다.
 */
@Slf4j
public abstract class KeyedEmitterRegistry {

    private final Map<Long, Map<String, SseEmitter>> registry = new ConcurrentHashMap<>();

    public SseEmitter register(Long key) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String connId = UUID.randomUUID().toString();

        registry
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(connId, emitter);

        emitter.onCompletion(() -> remove(key, connId));
        emitter.onTimeout(() -> remove(key, connId));
        emitter.onError(e -> remove(key, connId));

        log.info("SSE 연결 등록: key={}, connId={}", key, connId);
        return emitter;
    }

    public void send(Long key, NotificationEnvelope envelope) {
        Map<String, SseEmitter> conns = registry.get(key);
        if (conns == null || conns.isEmpty()) {
            return;
        }
        conns.forEach((connId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(envelope.sourceType().name())
                        .id(envelope.notificationId() == null ? "" : envelope.notificationId().toString())
                        .data(envelope));
            } catch (IOException e) {
                log.warn("SSE 전송 실패 — emitter 제거: key={}, connId={}", key, connId);
                remove(key, connId);
            }
        });
    }

    public void heartbeat() {
        registry.forEach((key, conns) ->
                conns.forEach((connId, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    } catch (IOException e) {
                        log.warn("Heartbeat 실패 — emitter 제거: key={}, connId={}", key, connId);
                        remove(key, connId);
                    }
                }));
    }

    public int connectionCount(Long key) {
        Map<String, SseEmitter> conns = registry.get(key);
        return conns == null ? 0 : conns.size();
    }

    private void remove(Long key, String connId) {
        Map<String, SseEmitter> conns = registry.get(key);
        if (conns != null) {
            conns.remove(connId);
            if (conns.isEmpty()) {
                registry.remove(key);
            }
        }
        log.info("SSE 연결 해제: key={}, connId={}", key, connId);
    }
}