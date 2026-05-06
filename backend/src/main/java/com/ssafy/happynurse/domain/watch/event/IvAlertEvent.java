package com.ssafy.happynurse.domain.watch.event;

import com.ssafy.happynurse.domain.watch.scheduler.AlertType;

import java.time.Instant;

/**
 * 수액 알림이 "발사돼야 함" 을 의미하는 도메인 이벤트
 * <p>
 * 발행자: {@code IvAlertScheduler#fire} — DB CAS 마킹 성공 시 같은 트랜잭션 안에서 publish
 * 구독자: {@code iv/listener/IvAlertNotificationAdapter} — {@code @TransactionalEventListener(AFTER_COMMIT)}
 * 로 받아 NotificationEnvelope 변환 후 dispatcher 에 전달
 */
public record IvAlertEvent(
        Long ivInfusionId,
        AlertType alertType,
        Instant firedAt
) {
}
