package com.ssafy.happynurse.domain.nurse.notification.api;

import lombok.Getter;

/**
 * Producer가 "어디까지 알림을 보낼지 + 어느 긴급도인지"를 선언적으로 표현하는 정책.
 * 모든 알림은 Notification 테이블에 영구 저장되므로 persist 플래그는 없다.
 *
 * - ALERT_CRITICAL / ASSIGN_DELIVERY 는 채널 조합이 같다 (전 채널). 차이는 "긴급도 라벨" —
 *   클라이언트가 표시 방식 (빨간 배너 vs 일반 토스트, 알림음 등) 분기에 사용.
 * - PERSONAL_INFO 는 본인 한정 — 데스크 PC 노출 없음, 본인 앱(SSE) + 본인 폰(FCM).
 */
@Getter
public enum PushPolicy {
    ALERT_CRITICAL  (true,  true, true),   // 긴급 — 환자에게 즉시 가야 함 (iv_alert)
    ASSIGN_DELIVERY (true,  true, true),   // 할당 업무 — 알림 받아 처리 (self_report, order_change)
    PERSONAL_INFO   (false, true, true);   // 본인 한정 리마인더 — 본인 앱/폰 (timer)

    private final boolean wardSse;
    private final boolean personalSse;
    private final boolean fcm;

    PushPolicy(boolean wardSse, boolean personalSse, boolean fcm) {
        this.wardSse = wardSse;
        this.personalSse = personalSse;
        this.fcm = fcm;
    }
}