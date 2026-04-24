package com.ssafy.happynurse.domain.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final long SEVEN_DAYS_MS = 604_800_000L;

    @Test
    @DisplayName("create() 호출 시 필드가 정상 설정된다")
    void create_정상_설정() {
        RefreshToken token = RefreshToken.create(
                "session-123", 1L, "EMP001", "홍길동",
                SEVEN_DAYS_MS, 1L, 2L, "nurse");

        assertThat(token.getTokenValue()).isNotNull().hasSize(36); // UUID format
        assertThat(token.getSessionId()).isEqualTo("session-123");
        assertThat(token.getPractitionerId()).isEqualTo(1L);
        assertThat(token.getEmployeeNumber()).isEqualTo("EMP001");
        assertThat(token.getPractitionerName()).isEqualTo("홍길동");
        assertThat(token.getOrganizationId()).isEqualTo(1L);
        assertThat(token.getWardId()).isEqualTo(2L);
        assertThat(token.getRoleCode()).isEqualTo("nurse");
        assertThat(token.getTtl()).isEqualTo(SEVEN_DAYS_MS);
    }

    @Test
    @DisplayName("create() 호출 시 매번 다른 tokenValue가 생성된다")
    void create_고유한_토큰값() {
        RefreshToken token1 = RefreshToken.create(
                "session-1", 1L, "EMP001", "홍길동",
                SEVEN_DAYS_MS, 1L, 2L, "nurse");
        RefreshToken token2 = RefreshToken.create(
                "session-1", 1L, "EMP001", "홍길동",
                SEVEN_DAYS_MS, 1L, 2L, "nurse");

        assertThat(token1.getTokenValue()).isNotEqualTo(token2.getTokenValue());
    }
}
