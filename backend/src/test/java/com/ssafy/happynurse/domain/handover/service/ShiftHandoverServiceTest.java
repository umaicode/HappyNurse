package com.ssafy.happynurse.domain.handover.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.handover.dto.HandoverDetailResponse;
import com.ssafy.happynurse.domain.handover.entity.ShiftHandover;
import com.ssafy.happynurse.domain.handover.repository.ShiftHandoverRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShiftHandoverServiceTest {

    @Mock ShiftHandoverRepository repository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ShiftHandoverService service;

    // ──── getDetail ────

    @Test
    @DisplayName("getDetail 성공 - handoverId 와 checkedItemsJson 을 반환한다")
    void getDetail_성공() {
        ShiftHandover h = mock(ShiftHandover.class);
        given(h.getHandoverId()).willReturn(42L);
        given(h.getCheckedItemsJson()).willReturn(Map.of(
                "synthesis.0", Map.of("by", 7L, "at", "2026-05-12T08:13:00")
        ));
        given(repository.findById(42L)).willReturn(Optional.of(h));

        HandoverDetailResponse result = service.getDetail(42L);

        assertThat(result.handoverId()).isEqualTo("42");
        assertThat(result.checkedItemsJson()).containsKey("synthesis.0");
    }

    @Test
    @DisplayName("getDetail - checkedItemsJson 이 null 이면 빈 맵을 반환한다")
    void getDetail_체크리스트_null() {
        ShiftHandover h = mock(ShiftHandover.class);
        given(h.getHandoverId()).willReturn(42L);
        given(h.getCheckedItemsJson()).willReturn(null);
        given(repository.findById(42L)).willReturn(Optional.of(h));

        HandoverDetailResponse result = service.getDetail(42L);

        assertThat(result.checkedItemsJson()).isEmpty();
    }

    @Test
    @DisplayName("getDetail 실패 - 존재하지 않는 handoverId → HANDOVER_NOT_FOUND")
    void getDetail_실패_없는_ID() {
        given(repository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(42L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HANDOVER_NOT_FOUND);
    }

    // ──── patchChecks ────

    @Test
    @DisplayName("patchChecks 성공 - true 키는 addChecks 로 머지된다")
    void patchChecks_체크_ON() throws Exception {
        given(repository.existsById(42L)).willReturn(true);
        given(objectMapper.writeValueAsString(any()))
                .willReturn("{\"synthesis.0\":{\"by\":7,\"at\":\"2026-05-12T08:13:00\"}}");

        service.patchChecks(42L, Map.of("synthesis.0", true), 7L);

        verify(repository).addChecks(eq(42L), anyString());
        verify(repository, never()).removeCheck(anyLong(), anyString());
    }

    @Test
    @DisplayName("patchChecks 성공 - false 키는 removeCheck 로 제거된다")
    void patchChecks_체크_OFF() {
        given(repository.existsById(42L)).willReturn(true);

        service.patchChecks(42L, Map.of("synthesis.0", false), 7L);

        verify(repository).removeCheck(42L, "synthesis.0");
        verify(repository, never()).addChecks(anyLong(), anyString());
    }

    @Test
    @DisplayName("patchChecks 성공 - true/false 가 섞이면 addChecks 와 removeCheck 가 모두 호출된다")
    void patchChecks_혼합() throws Exception {
        given(repository.existsById(42L)).willReturn(true);
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("synthesis.0", true);
        checks.put("synthesis.2", false);
        service.patchChecks(42L, checks, 7L);

        verify(repository).addChecks(eq(42L), anyString());
        verify(repository).removeCheck(42L, "synthesis.2");
    }

    @Test
    @DisplayName("patchChecks 실패 - synthesis 가 아닌 슬롯 키 → HANDOVER_CHECK_KEY_INVALID")
    void patchChecks_실패_다른_슬롯_키() {
        assertThatThrownBy(() -> service.patchChecks(42L, Map.of("pass.0", true), 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HANDOVER_CHECK_KEY_INVALID);

        verify(repository, never()).existsById(anyLong());
        verify(repository, never()).addChecks(anyLong(), anyString());
        verify(repository, never()).removeCheck(anyLong(), anyString());
    }

    @Test
    @DisplayName("patchChecks 실패 - synthesis.숫자 형식이 아니면 HANDOVER_CHECK_KEY_INVALID")
    void patchChecks_실패_index_가_숫자_아님() {
        assertThatThrownBy(() -> service.patchChecks(42L, Map.of("synthesis.abc", true), 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HANDOVER_CHECK_KEY_INVALID);
    }

    @Test
    @DisplayName("patchChecks 실패 - 존재하지 않는 handoverId → HANDOVER_NOT_FOUND")
    void patchChecks_실패_없는_ID() {
        given(repository.existsById(42L)).willReturn(false);

        assertThatThrownBy(() -> service.patchChecks(42L, Map.of("synthesis.0", true), 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HANDOVER_NOT_FOUND);

        verify(repository, never()).addChecks(anyLong(), anyString());
        verify(repository, never()).removeCheck(anyLong(), anyString());
    }
}
