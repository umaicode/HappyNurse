package com.ssafy.happynurse.domain.nurse.notification.service.fcm;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;

/**
 * 특정 간호사의 활성 모바일 디바이스(FCM 토큰)들에 envelope을 push한다.
 * Step 1은 NoOp. Step 3에서 Firebase Admin SDK 구현으로 교체.
 */
public interface FcmSender {
    void sendToActiveDevicesOf(Long practitionerId, NotificationEnvelope envelope);
}