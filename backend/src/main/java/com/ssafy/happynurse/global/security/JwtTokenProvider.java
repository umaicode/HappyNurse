package com.ssafy.happynurse.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.access-token-expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] decoded = Decoders.BASE64.decode(secretBase64);
        this.key = Keys.hmacShaKeyFor(decoded);
    }

    public String createAccessToken(Long practitionerId, String employeeNumber,
                                     String name, String roleCode, String sessionId,
                                     Long organizationId, Long wardId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(practitionerId))
                .claim("employeeNumber", employeeNumber)
                .claim("name", name)
                .claim("role", roleCode)
                .claim("sessionId", sessionId)
                .claim("organizationId", organizationId)
                .claim("wardId", wardId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public Long getPractitionerId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getSessionId(String token) {
        return getClaims(token).get("sessionId", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
