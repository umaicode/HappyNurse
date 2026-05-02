package com.ssafy.happynurse.domain.nurse.notification.api;

/**
 * 알림 라우팅 인프라의 단일 진입점.
 * Producer 도메인의 어댑터가 envelope을 만들어 dispatch()를 호출하면,
 * dispatcher가 영속화 후 PushPolicy에 선언된 채널들 (ward SSE / personal SSE / FCM) 로 fan-out한다.
 */
public interface NotificationDispatcher {
    void dispatch(NotificationEnvelope envelope);
}