package com.ssafy.happynurse.global.security;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.access-token-expiration-ms}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        throw new UnsupportedOperationException("미구현");
    }

    public String createAccessToken(Long practitionerId, String employeeNumber,
                                     String name, String roleCode, String sessionId) {
        throw new UnsupportedOperationException("미구현");
    }

    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("미구현");
    }

    public Claims getClaims(String token) {
        throw new UnsupportedOperationException("미구현");
    }

    public Long getPractitionerId(String token) {
        throw new UnsupportedOperationException("미구현");
    }

    public String getRole(String token) {
        throw new UnsupportedOperationException("미구현");
    }

    public String getSessionId(String token) {
        throw new UnsupportedOperationException("미구현");
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
