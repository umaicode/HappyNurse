package com.ssafy.happynurse.domain.nurse.notification.service.registry;

import org.springframework.stereotype.Component;

/**
 * key = wardId. 데스크 PC가 ward 단위로 SSE를 구독한다.
 * Step 1 시점에는 등록 엔드포인트가 없어 비어있는 상태로 유지된다 (Step 2에서 wiring).
 */
@Component
public class WardEmitterRegistry extends KeyedEmitterRegistry {
}