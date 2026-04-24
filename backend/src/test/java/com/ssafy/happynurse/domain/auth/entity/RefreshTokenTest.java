package com.ssafy.happynurse.domain.auth.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RefreshTokenTest {

    private static final long SEVEN_DAYS_MS = 604_800_000L;

    @Test
    @DisplayName("create() 호출 시 필드가 정상 설정된다")
    void create_정상_설정() {
        Practitioner practitioner = mock(Practitioner.class);

        RefreshToken token = RefreshToken.create(
                "session-123", practitioner, SEVEN_DAYS_MS, 1L, 2L, "NURSE");

        assertThat(token.getTokenValue()).isNotNull().hasSize(36); // UUID format
        assertThat(token.getSessionId()).isEqualTo("session-123");
        assertThat(token.getPractitioner()).isEqualTo(practitioner);
        assertThat(token.getOrganizationId()).isEqualTo(1L);
        assertThat(token.getWardId()).isEqualTo(2L);
        assertThat(token.getRoleCode()).isEqualTo("NURSE");
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
    }

    @Test
    @DisplayName("revoke() 호출 시 revoked가 true로 변경된다")
    void revoke_호출_시_true() {
        Practitioner practitioner = mock(Practitioner.class);
        RefreshToken token = RefreshToken.create(
                "session-123", practitioner, SEVEN_DAYS_MS, 1L, 2L, "NURSE");

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("isUsable() - 만료되지 않고 revoke되지 않으면 true")
    void isUsable_유효한_토큰() {
        Practitioner practitioner = mock(Practitioner.class);
        RefreshToken token = RefreshToken.create(
                "session-123", practitioner, SEVEN_DAYS_MS, 1L, 2L, "NURSE");

        assertThat(token.isUsable()).isTrue();
    }

    @Test
    @DisplayName("isUsable() - revoke된 토큰이면 false")
    void isUsable_revoke된_토큰() {
        Practitioner practitioner = mock(Practitioner.class);
        RefreshToken token = RefreshToken.create(
                "session-123", practitioner, SEVEN_DAYS_MS, 1L, 2L, "NURSE");

        token.revoke();

        assertThat(token.isUsable()).isFalse();
    }

    @Test
    @DisplayName("isUsable() - 만료된 토큰이면 false")
    void isUsable_만료된_토큰() throws Exception {
        Practitioner practitioner = mock(Practitioner.class);
        RefreshToken token = RefreshToken.create(
                "session-123", practitioner, SEVEN_DAYS_MS, 1L, 2L, "NURSE");

        // 리플렉션으로 expiresAt을 과거로 설정
        Field expiresAtField = RefreshToken.class.getDeclaredField("expiresAt");
        expiresAtField.setAccessible(true);
        expiresAtField.set(token, LocalDateTime.now().minusDays(1));

        assertThat(token.isUsable()).isFalse();
    }
}
