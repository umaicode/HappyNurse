package com.ssafy.happynurse.domain.nurse.notification.service.fcm;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 1 stub. 실제 전송 없이 로그만 남긴다.
 * Step 3에서 Firebase Admin SDK 기반 구현으로 교체될 때 이 빈을 제거한다.
 */
@Slf4j
@Component
public class NoOpFcmSender implements FcmSender {

    @Override
    public void sendToActiveDevicesOf(Long practitionerId, NotificationEnvelope envelope) {
        log.info("[NoOpFcm] would send to practitionerId={}, sourceType={}",
                practitionerId, envelope.sourceType());
    }
}