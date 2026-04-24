package com.ssafy.happynurse.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenReuseDetectorTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RefreshTokenReuseDetector reuseDetector;

    @Test
    @DisplayName("markAsRotated 호출 시 Redis에 sessionId를 저장한다")
    void markAsRotated_저장() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        reuseDetector.markAsRotated("token-abc", "session-1", 604800000L);

        verify(valueOps).set("rt:revoked:token-abc", "session-1", 604800000L, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("getReusedSessionId - 재사용된 토큰이면 sessionId를 반환한다")
    void getReusedSessionId_재사용_감지() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("rt:revoked:stolen-token")).willReturn("session-1");

        String sessionId = reuseDetector.getReusedSessionId("stolen-token");

        assertThat(sessionId).isEqualTo("session-1");
    }

    @Test
    @DisplayName("getReusedSessionId - 정상 토큰이면 null을 반환한다")
    void getReusedSessionId_정상() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("rt:revoked:valid-token")).willReturn(null);

        String sessionId = reuseDetector.getReusedSessionId("valid-token");

        assertThat(sessionId).isNull();
    }
}