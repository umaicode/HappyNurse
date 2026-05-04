package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.dto.MedicationItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemType;
import com.ssafy.happynurse.domain.nurse.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.service.NursingNoteService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NursingNoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class NursingNoteControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    NursingNoteService nursingNoteService;

    @BeforeEach
    void setUpSecurity() {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP-001", "김간호", "nurse", "session-1", 1L, 3L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /encounters/{id}/nursing-notes?date= - 조회 성공")
    void getNursingNotes_성공_200() throws Exception {
        NursingNoteItemResponse stt = new NursingNoteItemResponse(
                NursingNoteItemType.STT_NOTE,
                LocalDateTime.of(2026, 5, 3, 14, 30),
                RecordStatus.confirmed,
                6L, "이조은", true,
                12L, "환자 통증 호소",
                null, null, null);
        NursingNoteItemResponse med = new NursingNoteItemResponse(
                NursingNoteItemType.MEDICATION,
                LocalDateTime.of(2026, 5, 3, 14, 25),
                RecordStatus.confirmed,
                6L, "이조은", true,
                null, null,
                "tag-uuid-1", true,
                List.of(new MedicationItemResponse(
                        31L, "PC1", "모르핀황산염주사",
                        new BigDecimal("5.000"), "mg", 1, "IV")));

        given(nursingNoteService.getNursingNotes(eq(42L), eq(LocalDate.of(2026, 5, 3)), eq(1L), eq(3L)))
                .willReturn(List.of(stt, med));

        mockMvc.perform(get("/encounters/42/nursing-notes").param("date", "2026-05-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("STT_NOTE"))
                .andExpect(jsonPath("$.data[0].nursingRecordId").value(12))
                .andExpect(jsonPath("$.data[0].editable").value(true))
                .andExpect(jsonPath("$.data[1].type").value("MEDICATION"))
                .andExpect(jsonPath("$.data[1].taggingId").value("tag-uuid-1"))
                .andExpect(jsonPath("$.data[1].medications[0].productName").value("모르핀황산염주사"));
    }

    @Test
    @DisplayName("GET /encounters/{id}/nursing-notes - 입원 없으면 404")
    void getNursingNotes_실패_입원_없음_404() throws Exception {
        given(nursingNoteService.getNursingNotes(eq(99L), any(), any(), any()))
                .willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        mockMvc.perform(get("/encounters/99/nursing-notes").param("date", "2026-05-03"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ENCOUNTER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /encounters/{id}/nursing-notes - 타 병동 입원이면 403")
    void getNursingNotes_실패_타_병동_403() throws Exception {
        given(nursingNoteService.getNursingNotes(eq(42L), any(), any(), any()))
                .willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD));

        mockMvc.perform(get("/encounters/42/nursing-notes").param("date", "2026-05-03"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ENCOUNTER_NOT_IN_MY_WARD"));
    }
}
