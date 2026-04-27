package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.AppLoginResponse;
import com.ssafy.happynurse.domain.auth.dto.AppRefreshRequest;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginRequest;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "앱 인증", description = "앱 로그인, 로그아웃, 토큰 갱신 API (토큰을 쿠키가 아닌 응답 body로 반환)")
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AuthService authService;

    @Value("${jwt.app-refresh-token-expiration-ms}")
    private long appRefreshExpirationMs;

    @Operation(operationId = "appLogin", summary = "앱 로그인", description = "사원번호와 비밀번호로 로그인합니다. 성공 시 accessToken과 refreshToken을 응답 body로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "자격증명 오류 — INVALID_CREDENTIALS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기관 또는 병동 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 병동 권한 없음")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AppLoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.login(
                request.employeeNumber(),
                request.password(),
                httpRequest.getRemoteAddr(),
                request.organizationId(),
                request.wardId(),
                appRefreshExpirationMs
        );

        return ResponseEntity.ok(
                ApiResponse.ok("로그인에 성공했습니다.", AppLoginResponse.from(result)));
    }

    @Operation(operationId = "appRefresh", summary = "앱 토큰 갱신", description = "refreshToken으로 새로운 accessToken과 refreshToken을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰 — REFRESH_TOKEN_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "리프레시 토큰 재사용 감지 — REFRESH_TOKEN_REUSE_DETECTED")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AppLoginResponse>> refresh(
            @Valid @RequestBody AppRefreshRequest request) {

        AuthResult result = authService.refresh(request.refreshToken());

        return ResponseEntity.ok(
                ApiResponse.ok("토큰이 갱신되었습니다.", AppLoginResponse.from(result)));
    }

    @Operation(operationId = "appLogout", summary = "앱 로그아웃", description = "현재 세션을 종료합니다. Authorization 헤더에 accessToken이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logout(userDetails.getSessionId());

        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다.", null));
    }
}
