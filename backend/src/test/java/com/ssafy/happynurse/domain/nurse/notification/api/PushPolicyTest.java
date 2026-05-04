package com.ssafy.happynurse.domain.nurse.notification.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PushPolicyTest {

    @Test
    void alertCritical_enablesAllChannels() {
        PushPolicy p = PushPolicy.ALERT_CRITICAL;
        assertThat(p.isWardSse()).isTrue();
        assertThat(p.isPersonalSse()).isTrue();
        assertThat(p.isFcm()).isTrue();
    }

    @Test
    void assignDelivery_enablesAllChannels() {
        PushPolicy p = PushPolicy.ASSIGN_DELIVERY;
        assertThat(p.isWardSse()).isTrue();
        assertThat(p.isPersonalSse()).isTrue();
        assertThat(p.isFcm()).isTrue();
    }

    @Test
    void personalInfo_skipsWardSse_keepsPersonalAndFcm() {
        PushPolicy p = PushPolicy.PERSONAL_INFO;
        assertThat(p.isWardSse()).isFalse();
        assertThat(p.isPersonalSse()).isTrue();
        assertThat(p.isFcm()).isTrue();
    }
}