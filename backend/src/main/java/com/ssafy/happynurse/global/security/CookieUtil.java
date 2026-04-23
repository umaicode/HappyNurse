package com.ssafy.happynurse.global.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(String cookieName, String token,
                                                          long maxAgeMs, boolean secure, String sameSite) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(sameSite)
                .build();
    }

    public static ResponseCookie clearAccessTokenCookie(String cookieName, boolean secure, String sameSite) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}
