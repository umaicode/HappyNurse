package com.ssafy.happynurse.domain.nurse.notification.service.fcm;

import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.api.PushPolicy;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

class NoOpFcmSenderTest {

    @Test
    void send_doesNotThrow() {
        NoOpFcmSender sender = new NoOpFcmSender();
        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.self_report,
                1L, 7L, 100L, 50L,
                "title", "body",
                "payload", Instant.now(), null,
                PushPolicy.ASSIGN_DELIVERY);

        assertThatCode(() -> sender.sendToActiveDevicesOf(7L, envelope))
                .doesNotThrowAnyException();
    }
}