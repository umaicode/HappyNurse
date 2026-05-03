package com.ssafy.happynurse.domain.nurse.notification.service.fcm;

import com.google.firebase.messaging.*;
import com.ssafy.happynurse.domain.common.entity.PractitionerDevice;
import com.ssafy.happynurse.domain.common.repository.PractitionerDeviceRepository;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FirebaseFcmSender implements FcmSender {

    private final FirebaseMessaging firebaseMessaging;
    private final PractitionerDeviceRepository deviceRepository;

    @Override
    public void sendToActiveDevicesOf(Long practitionerId, NotificationEnvelope envelope) {
        List<PractitionerDevice> devices = deviceRepository.findActiveByPractitionerId(practitionerId);
        if (devices.isEmpty()) {
            log.info("[FCM] 활성 디바이스 없음 — practitionerId={}, sourceType={}",
                    practitionerId, envelope.sourceType());
            return;
        }

        List<String> tokens = devices.stream().map(PractitionerDevice::getFcmToken).toList();
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(envelope.title())
                        .setBody(envelope.body())
                        .build())
                .putAllData(buildDataPayload(envelope))
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("[FCM] practitionerId={} 발송 결과: success={}, failure={}",
                    practitionerId, response.getSuccessCount(), response.getFailureCount());
            // 응답 처리 (성공 토큰 last_used_at 갱신, 실패 토큰 비활성화) → Task 6 에서
        } catch (FirebaseMessagingException e) {
            log.warn("[FCM] practitionerId={} 발송 호출 자체 실패", practitionerId, e);
        }
    }

    private Map<String, String> buildDataPayload(NotificationEnvelope env) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(env.notificationId()));
        data.put("sourceType", env.sourceType().name());
        if (env.patientId() != null) data.put("patientId", String.valueOf(env.patientId()));
        if (env.sourceEntityId() != null) data.put("sourceEntityId", String.valueOf(env.sourceEntityId()));
        return data;
    }
}