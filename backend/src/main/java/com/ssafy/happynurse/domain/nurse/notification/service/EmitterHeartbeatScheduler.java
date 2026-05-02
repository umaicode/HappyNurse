package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.PersonalEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 30초 주기로 모든 SSE 연결에 heartbeat ping 송신.
 * 좀비 연결을 자동 정리하고 로드밸런서 idle timeout을 회피한다.
 */
@Component
@RequiredArgsConstructor
public class EmitterHeartbeatScheduler {

    private final WardEmitterRegistry wardEmitterRegistry;
    private final PersonalEmitterRegistry personalEmitterRegistry;

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        wardEmitterRegistry.heartbeat();
        personalEmitterRegistry.heartbeat();
    }
}