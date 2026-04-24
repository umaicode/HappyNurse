package com.ssafy.happynurse.global.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-key-for-jwt-256bit!".getBytes());
    private static final long EXPIRATION_MS = 1800000L;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();
        setField(jwtTokenProvider, "secretBase64", SECRET);
        setField(jwtTokenProvider, "expirationMs", EXPIRATION_MS);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("토큰 생성 시 null이 아닌 문자열을 반환한다")
    void createAccessToken_정상_토큰_생성() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 true를 반환한다")
    void validateToken_유효한_토큰_true() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 시 false를 반환한다")
    void validateToken_잘못된_토큰_false() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 false를 반환한다")
    void validateToken_만료된_토큰_false() throws Exception {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        setField(expiredProvider, "secretBase64", SECRET);
        setField(expiredProvider, "expirationMs", 0L);
        expiredProvider.init();

        String token = expiredProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("토큰에서 Claims를 정상적으로 추출한다")
    void getClaims_정상_추출() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        Claims claims = jwtTokenProvider.getClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("employeeNumber", String.class)).isEqualTo("EMP001");
        assertThat(claims.get("name", String.class)).isEqualTo("홍길동");
        assertThat(claims.get("role", String.class)).isEqualTo("nurse");
        assertThat(claims.get("sessionId", String.class)).isEqualTo("session-123");
    }

    @Test
    @DisplayName("토큰에서 practitionerId를 추출한다")
    void getPractitionerId_정상_추출() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(jwtTokenProvider.getPractitionerId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("토큰에서 role을 추출한다")
    void getRole_정상_추출() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("nurse");
    }

    @Test
    @DisplayName("토큰에서 sessionId를 추출한다")
    void getSessionId_정상_추출() {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);

        assertThat(jwtTokenProvider.getSessionId(token)).isEqualTo("session-123");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
