package com.ssafy.happynurse.domain.nurse.notification.api;

import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;

import java.time.Instant;

/**
 * 모든 도메인 이벤트가 dispatcher에 들어올 때 변환되는 표준 형태.
 import java.time.Instant;

 /**
 * 모든 도메인 이벤트가 dispatcher에 들어올 때 변환되는 표준 형태.
 *
 * - wardId, assignedPractitionerId 항상 필수 (현재 4개 카탈로그 모두 담당자 있음)
 * - sourceEntityId: producer 도메인 PK (PatientSelfReport.id 등). Notification 테이블의
 *   도메인별 source FK 컬럼에 저장됨 (각 도메인 담당자가 컬럼 추가)
 * - title, body: Notification.title / Notification.body 매핑
 * - payload: SSE/FCM 직렬화용 도메인 DTO (DB 저장 X)
 * - notificationId: producer는 null로 두기. dispatcher가 영속화 후 채움.
 */
public record NotificationEnvelope(
        SourceType      sourceType,
        Long            wardId,
        Long            assignedPractitionerId,
        Long            patientId,
        Long            sourceEntityId,
        String          title,
        String          body,
        Object          payload,
        Instant         occurredAt,
        Long            notificationId,
        PushPolicy      pushPolicy,
        SymptomPriority priority
) {
    /** dispatcher가 영속화 후 notificationId를 채워 새 envelope을 만들 때 사용. */
    public NotificationEnvelope withNotificationId(Long notificationId) {
        return new NotificationEnvelope(
                sourceType, wardId, assignedPractitionerId, patientId, sourceEntityId,
                title, body, payload, occurredAt, notificationId, pushPolicy, priority);
    }
}