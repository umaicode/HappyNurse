package com.ssafy.happynurse.global.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(String cookieName, String token,
                                                          long maxAgeMs, boolean secure, String sameSite) {
        return createTokenCookie(cookieName, token, maxAgeMs, secure, sameSite, "/");
    }

    public static ResponseCookie clearAccessTokenCookie(String cookieName, boolean secure, String sameSite) {
        return clearTokenCookie(cookieName, secure, sameSite, "/");
    }

    public static ResponseCookie createTokenCookie(String cookieName, String token,
                                                    long maxAgeMs, boolean secure,
                                                    String sameSite, String path) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(sameSite)
                .build();
    }

    public static ResponseCookie clearTokenCookie(String cookieName, boolean secure,
                                                   String sameSite, String path) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}
