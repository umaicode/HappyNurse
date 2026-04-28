package com.ssafy.happynurse.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.DevLoginRequest;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DevAuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "app.dev.enabled=true",
        "jwt.refresh-token-expiration-ms=604800000"
})
class DevAuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;

    private static final LoginResponse LOGIN_RESPONSE = new LoginResponse(
            1L, "홍길동", "EMP001", "nurse", 1L, 3L);

    private static final AuthResult AUTH_RESULT = new AuthResult(
            "mock-access-token", "mock-refresh-token", LOGIN_RESPONSE);

    @Test
    @DisplayName("POST /auth/dev-login 성공 시 200 + 토큰을 body로 반환, refreshExpirationMs가 service에 전달된다")
    void devLogin_성공() throws Exception {
        given(authService.devLogin(anyString(), anyString(), anyLong()))
                .willReturn(AUTH_RESULT);

        DevLoginRequest request = new DevLoginRequest("EMP001");

        mockMvc.perform(post("/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.practitionerId").value(1))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.roleCode").value("nurse"));

        verify(authService).devLogin(eq("EMP001"), anyString(), eq(604800000L));
    }

    @Test
    @DisplayName("POST /auth/dev-login - 존재하지 않는 사원번호 → 404 EMPLOYEE_NUMBER_NOT_FOUND")
    void devLogin_실패_사원번호_없음() throws Exception {
        given(authService.devLogin(anyString(), anyString(), anyLong()))
                .willThrow(new CustomException(ErrorCode.EMPLOYEE_NUMBER_NOT_FOUND));

        DevLoginRequest request = new DevLoginRequest("UNKNOWN");

        mockMvc.perform(post("/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NUMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /auth/dev-login - 활성 역할 없음(퇴사) → 403 ACCOUNT_DISABLED")
    void devLogin_실패_활성_역할_없음() throws Exception {
        given(authService.devLogin(anyString(), anyString(), anyLong()))
                .willThrow(new CustomException(ErrorCode.ACCOUNT_DISABLED));

        DevLoginRequest request = new DevLoginRequest("RETIRED");

        mockMvc.perform(post("/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("POST /auth/dev-login - 사원번호 누락 시 400")
    void devLogin_실패_입력값() throws Exception {
        DevLoginRequest request = new DevLoginRequest("");

        mockMvc.perform(post("/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
