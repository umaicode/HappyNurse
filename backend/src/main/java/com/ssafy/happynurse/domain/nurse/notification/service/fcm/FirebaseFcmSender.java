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
import java.util.Map.Entry;

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
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH) // 워치 풀스크린 알람 도달성 강화
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("[FCM] practitionerId={} 발송 결과: success={}, failure={}",
                    practitionerId, response.getSuccessCount(), response.getFailureCount());

            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                PractitionerDevice device = devices.get(i);
                if (r.isSuccessful()) {
                    device.touchLastUsed();
                } else {
                    handleFailure(device, r.getException());
                }
            }
        } catch (FirebaseMessagingException e) {
            log.warn("[FCM] practitionerId={} 발송 호출 자체 실패", practitionerId, e);
        }
    }

    private void handleFailure(PractitionerDevice device, FirebaseMessagingException ex) {
        MessagingErrorCode code = ex.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            log.info("[FCM] 토큰 비활성화: deviceId={}, errorCode={}", device.getDeviceId(), code);
            device.deactivate();
        } else {
            log.warn("[FCM] 일시적 발송 실패 (비활성화 X): deviceId={}, errorCode={}",
                    device.getDeviceId(), code, ex);
        }
    }

    Map<String, String> buildDataPayload(NotificationEnvelope env) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(env.notificationId()));
        data.put("sourceType", env.sourceType().name());
        if (env.patientId() != null) data.put("patientId", String.valueOf(env.patientId()));
        if (env.sourceEntityId() != null) data.put("sourceEntityId", String.valueOf(env.sourceEntityId()));
        if (env.priority() != null) data.put("priority", env.priority().name());

        // envelope.payload 가 Map<String,String> 이면 추가 키들을 그대로 전달
        // (워치 SttAlarmActivity 등이 patientName, contentSummary, roomBedTime 등을 읽음)
        if (env.payload() instanceof Map<?, ?> payloadMap) {
            for (Entry<?, ?> e : payloadMap.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                String key = e.getKey().toString();
                if (data.containsKey(key)) continue; // 표준 키 보호
                data.put(key, e.getValue().toString());
            }
        }
        return data;
    }
}