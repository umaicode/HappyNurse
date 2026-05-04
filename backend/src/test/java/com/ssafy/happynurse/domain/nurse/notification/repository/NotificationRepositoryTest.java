package com.ssafy.happynurse.domain.nurse.notification.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("@DataJpaTest 시드 셋업 비용 회피 — Task 5 통합 동작 검증 (dev DB curl) 으로 대체")
class NotificationRepositoryTest {

    @Test
    void findByWardIdWithCursor_returnsLatestFirst() {
        // ward → room → encounter(in_progress) → patient → notification 시드 후
        // findByWardIdWithCursor(wardId, since=null, before=null, limit=10) 호출
        // → notificationId DESC 정렬 검증
    }

    @Test
    void findByWardIdWithCursor_filtersByBefore() {
        // before=t2.notificationId → 결과 [t1] 만 (t2, t3 제외)
    }

    @Test
    void findByWardIdWithCursor_filtersBySince() {
        // since=t2.createdAt → 결과 [t3, t2]
    }

    @Test
    void findByPractitionerIdWithCursor_filtersByRecipient() {
        // 같은 ward 에 nurse / otherNurse 알림 각 1건
        // findByRecipientPractitionerIdWithCursor(nurse.id, ...) → nurse 것만
    }
}