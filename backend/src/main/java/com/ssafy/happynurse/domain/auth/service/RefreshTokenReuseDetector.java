package com.ssafy.happynurse.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenReuseDetector {

    private static final String REVOKED_PREFIX = "rt:revoked:";

    private final RedisTemplate<String, String> redisTemplate;

    public void markAsRotated(String tokenValue, String sessionId, long ttlMs) {
        String key = REVOKED_PREFIX + tokenValue;
        redisTemplate.opsForValue().set(key, sessionId, ttlMs, TimeUnit.MILLISECONDS);
    }

    public String getReusedSessionId(String tokenValue) {
        String key = REVOKED_PREFIX + tokenValue;
        return redisTemplate.opsForValue().get(key);
    }
}