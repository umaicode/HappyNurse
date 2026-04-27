package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.SseNotificationPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    // key: practitionerId, value: SseEmitter -> 연결된 간호사
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 간호사가 SSE 구독 시 호출. 간호사의 emitter를 등록한다.
    public SseEmitter register(Long practitionerId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.put(practitionerId, emitter);

        // 연결 종료/오류 시 자동 제거
        emitter.onCompletion(() -> remove(practitionerId));
        emitter.onTimeout(() -> remove(practitionerId));
        emitter.onError(e -> remove(practitionerId));

        log.info("SSE 구독 등록: practitionerId={}", practitionerId);

        return emitter;
    }

    // 담당 간호사 1명에게 직접 이벤트를 전송한다.
    // 간호사가 오프라인이면 스킵
    public void sendTo(Long practitionerId, SseNotificationPayload payload) {
        SseEmitter emitter = emitters.get(practitionerId);
        if (emitter == null) {
            log.info("SSE 전송 스킵 (오프라인): practitionerId={}", practitionerId);
            return;
        }
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException e) {
            log.warn("SSE 전송 실패: practitionerId={}", practitionerId);
            remove(practitionerId);
        }
    }

    @Scheduled(fixedRate = 30000)   // 30초마다 실행
    public void sendHeartbeat() {
        emitters.forEach((practitionerId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                log.warn("Heartbeat 실패, 연결 제거: practitionerId={}", practitionerId);
                remove(practitionerId);
            }
        });
    }

    private void remove(Long practitionerId) {
        emitters.remove(practitionerId);
        log.info("SSE 연결 해제: practitionerId={}", practitionerId);
    }
}
