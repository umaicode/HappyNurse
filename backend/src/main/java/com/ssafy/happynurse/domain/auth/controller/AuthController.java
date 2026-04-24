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

@RestController
@RequestMapping("/api/auth")
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.login(
                request.employeeNumber(),
                request.password(),
                httpRequest.getRemoteAddr(),
                request.organizationId(),
                request.wardId()
        );

        ResponseCookie accessCookie = CookieUtil.createAccessTokenCookie(
                cookieName, result.accessToken(), expirationMs, cookieSecure, cookieSameSite);
        ResponseCookie refreshCookie = CookieUtil.createTokenCookie(
                refreshCookieName, result.refreshToken(), refreshExpirationMs,
                cookieSecure, cookieSameSite, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("로그인에 성공했습니다.", result.loginResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logout(userDetails.getSessionId());

        ResponseCookie accessCookie = CookieUtil.clearAccessTokenCookie(
                cookieName, cookieSecure, cookieSameSite);
        ResponseCookie refreshCookie = CookieUtil.clearTokenCookie(
                refreshCookieName, cookieSecure, cookieSameSite, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok("로그아웃되었습니다.", null));
    }

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
                cookieSecure, cookieSameSite, "/api/auth");

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
