package com.ssafy.happynurse.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, "ACCESS_TOKEN");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ──── Bearer 헤더 ────

    @Test
    @DisplayName("Authorization Bearer 헤더에 유효한 토큰이 있으면 SecurityContext에 인증이 설정된다")
    void doFilter_Bearer헤더_유효한토큰_인증설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        stubValidClaims("valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(userDetails.getPractitionerId()).isEqualTo(1L);
        assertThat(userDetails.getEmployeeNumber()).isEqualTo("EMP001");
        assertThat(userDetails.getRole()).isEqualTo("nurse");
        assertThat(userDetails.getSessionId()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("Authorization 헤더에 Bearer 접두사가 없으면 토큰으로 인식하지 않는다")
    void doFilter_Bearer접두사없음_인증설정안됨() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token some-value");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 Bearer 토큰이면 SecurityContext가 비어있다")
    void doFilter_Bearer헤더_유효하지않은토큰_인증설정안됨() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ──── 쿠키 ────

    @Test
    @DisplayName("쿠키에 유효한 토큰이 있으면 SecurityContext에 인증이 설정된다")
    void doFilter_쿠키_유효한토큰_인증설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("ACCESS_TOKEN", "cookie-token"));
        stubValidClaims("cookie-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("쿠키와 Bearer 헤더가 모두 있으면 쿠키를 우선한다")
    void doFilter_쿠키와Bearer헤더_둘다있으면_쿠키우선() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("ACCESS_TOKEN", "cookie-token"));
        request.addHeader("Authorization", "Bearer header-token");
        stubValidClaims("cookie-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(userDetails.getPractitionerId()).isEqualTo(1L);
    }

    // ──── 토큰 없음 ────

    @Test
    @DisplayName("쿠키와 Bearer 헤더 모두 없으면 SecurityContext가 비어있다")
    void doFilter_토큰없음_인증설정안됨() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ──── 헬퍼 ────

    private void stubValidClaims(String token) {
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getClaims(token)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("employeeNumber", String.class)).willReturn("EMP001");
        given(claims.get("name", String.class)).willReturn("홍길동");
        given(claims.get("role", String.class)).willReturn("nurse");
        given(claims.get("sessionId", String.class)).willReturn("session-123");
        given(claims.get("organizationId", Long.class)).willReturn(1L);
        given(claims.get("wardId", Long.class)).willReturn(3L);
    }
}
