package com.ssafy.happynurse.global.security;

import com.ssafy.happynurse.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-key-for-jwt-256bit!".getBytes());
    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();
        setField(jwtTokenProvider, "secretBase64", SECRET);
        setField(jwtTokenProvider, "expirationMs", 1800000L);
        setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L);
        jwtTokenProvider.init();

        filter = new JwtAuthenticationFilter(jwtTokenProvider, COOKIE_NAME);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보가 설정된다")
    void 유효한_토큰_인증_설정() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-1", 1L, 3L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(request.getAttribute("authError")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("만료된 토큰이면 authError에 TOKEN_EXPIRED가 설정된다")
    void 만료된_토큰_TOKEN_EXPIRED() throws Exception {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        setField(expiredProvider, "secretBase64", SECRET);
        setField(expiredProvider, "expirationMs", 0L);
        setField(expiredProvider, "refreshTokenExpirationMs", 604800000L);
        expiredProvider.init();

        String expiredToken = expiredProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-1", 1L, 3L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, expiredToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("authError")).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 토큰이면 authError에 TOKEN_INVALID가 설정된다")
    void 잘못된_토큰_TOKEN_INVALID() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, "invalid.token.value"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("authError")).isEqualTo(ErrorCode.TOKEN_INVALID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 없으면 authError 설정 없이 통과한다")
    void 토큰_없음_통과() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("authError")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
