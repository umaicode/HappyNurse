package com.ssafy.happynurse.domain.patient.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** 간호사별 담당 환자 선택 영속 저장소 (시프트 교대로 DB 가 덮어써져도 본인 선택을 복원하기 위함). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignedPatientsCache {

    private static final String KEY_PREFIX = "assigned-patients:nurse:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final TypeReference<LinkedHashSet<Long>> TYPE_REF = new TypeReference<>() {};

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<Set<Long>> read(Long practitionerId, Long wardId) {
        String json = redisTemplate.opsForValue().get(key(practitionerId, wardId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            Set<Long> ids = objectMapper.readValue(json, TYPE_REF);
            return Optional.of(ids);
        } catch (JsonProcessingException e) {
            log.warn("AssignedPatientsCache 역직렬화 실패. practitionerId={}, wardId={}, json={}",
                    practitionerId, wardId, json, e);
            return Optional.empty();
        }
    }

    public void write(Long practitionerId, Long wardId, Collection<Long> encounterIds) {
        Set<Long> ordered = new LinkedHashSet<>(
                encounterIds == null ? Collections.emptyList() : encounterIds);
        try {
            String json = objectMapper.writeValueAsString(ordered);
            redisTemplate.opsForValue().set(key(practitionerId, wardId), json, TTL);
        } catch (JsonProcessingException e) {
            log.error("AssignedPatientsCache 직렬화 실패. practitionerId={}, wardId={}",
                    practitionerId, wardId, e);
        }
    }

    private String key(Long practitionerId, Long wardId) {
        return KEY_PREFIX + practitionerId + ":ward:" + wardId;
    }
}
