package com.ssafy.happynurse.domain.handover.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.handover.dto.HandoverDetailResponse;
import com.ssafy.happynurse.domain.handover.dto.WardEventsResponse;
import com.ssafy.happynurse.domain.handover.repository.ShiftHandoverRepository;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftHandoverService {

    private final ShiftHandoverRepository repository;
    private final EncounterRepository encounterRepository;
    private final ObjectMapper objectMapper;

    // 현재 체크박스가 붙는 슬롯은 synthesis (다른 슬롯 키가 들어오면 400)
    private static final Pattern CHECK_KEY_PATTERN = Pattern.compile("^synthesis\\.\\d+$");

    public HandoverDetailResponse getDetail(Long handoverId) {
        return repository.findById(handoverId)
                .map(HandoverDetailResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.HANDOVER_NOT_FOUND));
    }

    /**
     * 체크리스트 토글
     * - true 키: jsonb || 로 추가/덮어쓰기 ({ by: practitionerId, at: now })
     * - false 키: jsonb - key 로 제거
     * 한 트랜잭션 안에서 처리 (동시 호출이 와도 row-lock + jsonb 연산자가 atomic)
     */
    @Transactional
    public void patchChecks(Long handoverId, Map<String, Boolean> checks, Long practitionerId) {
        for (String key : checks.keySet()) {
            if (!CHECK_KEY_PATTERN.matcher(key).matches()) {
                throw new CustomException(ErrorCode.HANDOVER_CHECK_KEY_INVALID,
                        "허용되지 않는 키: " + key);
            }
        }

        if (!repository.existsById(handoverId)) {
            throw new CustomException(ErrorCode.HANDOVER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> addMap = new LinkedHashMap<>();
        java.util.List<String> removeKeys = new java.util.ArrayList<>();

        for (Map.Entry<String, Boolean> entry : checks.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                addMap.put(entry.getKey(), Map.of("by", practitionerId, "at", now.toString()));
            } else {
                removeKeys.add(entry.getKey());
            }
        }

        if (!addMap.isEmpty()) {
            try {
                repository.addChecks(handoverId, objectMapper.writeValueAsString(addMap));
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "checked_items_json 직렬화 실패");
            }
        }
        for (String key : removeKeys) {
            repository.removeCheck(handoverId, key);
        }
    }

    /**
     * 오늘(00 ~ 24) 해당 병동에 찍힌 입원/퇴원 환자 목록
     * 인수인계 화면에서 신규 입원·퇴원 리스트 표시용
     */
    public WardEventsResponse getTodayWardEvents(Long wardId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<WardEventsResponse.AdmissionItem> admissions = encounterRepository
                .findAdmissionsByWardAndPeriod(wardId, start, end)
                .stream()
                .map(WardEventsResponse.AdmissionItem::from)
                .toList();

        List<WardEventsResponse.DischargeItem> discharges = encounterRepository
                .findDischargesByWardAndPeriod(wardId, start, end)
                .stream()
                .map(WardEventsResponse.DischargeItem::from)
                .toList();

        return new WardEventsResponse(admissions, discharges);
    }
}
