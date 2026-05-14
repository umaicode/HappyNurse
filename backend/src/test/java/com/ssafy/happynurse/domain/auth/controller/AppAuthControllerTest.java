package com.ssafy.happynurse.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.auth.dto.AppLoginResponse;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginRequest;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppAuthControllerTest {

    @Mock AuthService authService;
    @InjectMocks AppAuthController appAuthController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appAuthController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ──── 로그인 ────

    @Test
    @DisplayName("앱 로그인 성공 시 accessToken과 refreshToken이 응답 body에 포함된다")
    void login_성공_accessToken이_body에_포함() throws Exception {
        LoginResponse loginResponse = new LoginResponse(1L, "홍길동", "EMP001", "nurse", 1L, 3L);
        AuthResult authResult = new AuthResult("mock-jwt-token", "mock-refresh-token", loginResponse);
        given(authService.loginApp(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .willReturn(authResult);

        LoginRequest request = new LoginRequest(1L, 3L, "EMP001", "password");

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.practitionerId").value(1))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.data.roleCode").value("nurse"))
                .andExpect(jsonPath("$.data.organizationId").value(1))
                .andExpect(jsonPath("$.data.wardId").value(3));
    }

    @Test
    @DisplayName("앱 로그인 성공 시 Set-Cookie 헤더가 응답에 없다")
    void login_성공_SetCookie헤더_없음() throws Exception {
        LoginResponse loginResponse = new LoginResponse(1L, "홍길동", "EMP001", "nurse", 1L, 3L);
        AuthResult authResult = new AuthResult("mock-jwt-token", "mock-refresh-token", loginResponse);
        given(authService.loginApp(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .willReturn(authResult);

        LoginRequest request = new LoginRequest(1L, 3L, "EMP001", "password");

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("organizationId 누락 시 400을 반환한다")
    void login_실패_organizationId누락_400() throws Exception {
        String body = """
                {"wardId": 3, "employeeNumber": "EMP001", "password": "password"}
                """;

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("wardId 누락 시 400을 반환한다")
    void login_실패_wardId누락_400() throws Exception {
        String body = """
                {"organizationId": 1, "employeeNumber": "EMP001", "password": "password"}
                """;

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("employeeNumber 누락 시 400을 반환한다")
    void login_실패_employeeNumber누락_400() throws Exception {
        String body = """
                {"organizationId": 1, "wardId": 3, "password": "password"}
                """;

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("password 누락 시 400을 반환한다")
    void login_실패_password누락_400() throws Exception {
        String body = """
                {"organizationId": 1, "wardId": 3, "employeeNumber": "EMP001"}
                """;

        mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ──── 로그아웃 ────

    @Test
    @DisplayName("앱 로그아웃 성공 시 200과 성공 메시지를 반환한다")
    void logout_성공() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        mockMvc.perform(post("/app/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));

        verify(authService).logout("session-123");
    }

    @Test
    @DisplayName("앱 로그아웃 성공 시 Set-Cookie 헤더가 응답에 없다")
    void logout_성공_SetCookie헤더_없음() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP001", "홍길동", "nurse", "session-123", 1L, 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        mockMvc.perform(post("/app/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
