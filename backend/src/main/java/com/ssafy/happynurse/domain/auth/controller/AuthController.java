package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginRequest;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.CookieUtil;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.access-token-expiration-ms}")
    private long expirationMs;

    @Value("${jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site}")
    private String cookieSameSite;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${jwt.refresh-cookie-name}")
    private String refreshCookieName;

    @Operation(summary = "로그인", description = "사원번호와 비밀번호로 로그인합니다. 성공 시 ACCESS_TOKEN, REFRESH_TOKEN 쿠키가 설정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "자격증명 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.login(
                request.employeeNumber(),
                request.password(),
                httpRequest.getRemoteAddr(),
                request.organizationId(),
                request.wardId(),
                refreshExpirationMs
        );

        ResponseCookie accessCookie = CookieUtil.createAccessTokenCookie(
                cookieName, result.accessToken(), expirationMs, cookieSecure, cookieSameSite);
        ResponseCookie refreshCookie = CookieUtil.createTokenCookie(
                refreshCookieName, result.refreshToken(), refreshExpirationMs,
                cookieSecure, cookieSameSite, "/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("로그인에 성공했습니다.", result.loginResponse()));
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 인증 쿠키를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logout(userDetails.getSessionId());

        ResponseCookie accessCookie = CookieUtil.clearAccessTokenCookie(
                cookieName, cookieSecure, cookieSameSite);
        ResponseCookie refreshCookie = CookieUtil.clearTokenCookie(
                refreshCookieName, cookieSecure, cookieSameSite, "/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("로그아웃되었습니다.", null));
    }

    @Operation(summary = "세션 연장", description = "현재 로그인된 사용자가 세션을 연장합니다. ACCESS_TOKEN 쿠키만 재발급되고 REFRESH_TOKEN은 그대로 유지됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 연장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/extend")
    public ResponseEntity<ApiResponse<Void>> extend(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String newAccessToken = authService.extend(userDetails);

        ResponseCookie accessCookie = CookieUtil.createAccessTokenCookie(
                cookieName, newAccessToken, expirationMs, cookieSecure, cookieSameSite);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ApiResponse.ok("세션이 연장되었습니다.", null));
    }

    @Operation(summary = "토큰 갱신", description = "REFRESH_TOKEN 쿠키로 새로운 ACCESS_TOKEN과 REFRESH_TOKEN을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshTokenValue = extractCookie(request, refreshCookieName);
        if (refreshTokenValue == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        AuthResult result = authService.refresh(refreshTokenValue);

        ResponseCookie accessCookie = CookieUtil.createAccessTokenCookie(
                cookieName, result.accessToken(), expirationMs, cookieSecure, cookieSameSite);
        ResponseCookie refreshCookie = CookieUtil.createTokenCookie(
                refreshCookieName, result.refreshToken(), refreshExpirationMs,
                cookieSecure, cookieSameSite, "/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("토큰이 갱신되었습니다.", result.loginResponse()));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
