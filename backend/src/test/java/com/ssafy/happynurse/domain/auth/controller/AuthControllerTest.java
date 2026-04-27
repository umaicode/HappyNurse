package com.ssafy.happynurse.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginRequest;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "jwt.cookie-name=ACCESS_TOKEN",
        "jwt.access-token-expiration-ms=1800000",
        "jwt.cookie-secure=false",
        "jwt.cookie-same-site=Lax",
        "jwt.refresh-token-expiration-ms=604800000",
        "jwt.refresh-cookie-name=REFRESH_TOKEN"
})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    private static final LoginResponse LOGIN_RESPONSE = new LoginResponse(
            1L, "홍길동", "EMP001", "nurse", 1L, 3L);

    private static final AuthResult AUTH_RESULT = new AuthResult(
            "mock-access-token", "mock-refresh-token", LOGIN_RESPONSE);

    // ──── 로그인 ────

    @Test
    @DisplayName("POST /auth/login 성공 시 200 + 쿠키 2개 + JSON body")
    void login_성공_쿠키2개_설정() throws Exception {
        given(authService.login(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .willReturn(AUTH_RESULT);

        LoginRequest request = new LoginRequest(1L, 3L, "EMP001", "password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.practitionerId").value(1))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.roleCode").value("nurse"))
                .andExpect(cookie().value("ACCESS_TOKEN", "mock-access-token"))
                .andExpect(cookie().value("REFRESH_TOKEN", "mock-refresh-token"))
                .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
                .andExpect(cookie().httpOnly("REFRESH_TOKEN", true));
    }

    @Test
    @DisplayName("POST /auth/login 실패 - 필수 필드 누락 시 400")
    void login_실패_잘못된_입력() throws Exception {
        LoginRequest request = new LoginRequest(null, null, "", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/login 실패 - 잘못된 자격증명 시 401")
    void login_실패_잘못된_자격증명() throws Exception {
        given(authService.login(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .willThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS));

        LoginRequest request = new LoginRequest(1L, 3L, "EMP001", "wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    // ──── 리프레시 ────

    @Test
    @DisplayName("POST /auth/refresh 성공 시 200 + 쿠키 2개 갱신")
    void refresh_성공_쿠키2개_갱신() throws Exception {
        given(authService.refresh("old-refresh-token")).willReturn(AUTH_RESULT);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.practitionerId").value(1))
                .andExpect(cookie().value("ACCESS_TOKEN", "mock-access-token"))
                .andExpect(cookie().value("REFRESH_TOKEN", "mock-refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/refresh 실패 - 쿠키 없음 시 401")
    void refresh_실패_쿠키_없음() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));
    }

    // ──── 로그아웃 ────

    @Test
    @DisplayName("POST /auth/logout 성공 시 200 + 쿠키 2개 삭제")
    void logout_성공_쿠키2개_삭제() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().maxAge("ACCESS_TOKEN", 0))
                .andExpect(cookie().maxAge("REFRESH_TOKEN", 0));

        verify(authService).logout("session-123");

        SecurityContextHolder.clearContext();
    }
}
