package com.ssafy.happynurse.domain.nurse.notification.service.registry;

import org.springframework.stereotype.Component;

/**
 * key = practitionerId. 모바일 앱 및 기존 /sse/subscribe 가 사용한다.
 */
@Component
public class PersonalEmitterRegistry extends KeyedEmitterRegistry {
}