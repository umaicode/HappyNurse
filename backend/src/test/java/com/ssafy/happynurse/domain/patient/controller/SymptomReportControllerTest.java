package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.SymptomReportItemResponse;
import com.ssafy.happynurse.domain.patient.dto.SymptomReportListResponse;
import com.ssafy.happynurse.domain.patient.service.SymptomReportService;
import com.ssafy.happynurse.domain.webapp.entity.InputMethod;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SymptomReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class SymptomReportControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    SymptomReportService symptomReportService;

    static final Long WARD_ID = 3L;

    @BeforeEach
    void setUpSecurity() {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP-001", "김간호", "nurse", "session-1", 1L, WARD_ID);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /patients/{patientId}/symptoms?date= - 조회 성공")
    void getSymptoms_성공() throws Exception {
        SymptomReportItemResponse item = new SymptomReportItemResponse(
                1L, InputMethod.quick_button, "통증", "통증",
                LocalDateTime.of(2026, 4, 29, 10, 30));
        SymptomReportListResponse response = new SymptomReportListResponse(
                1L, "이승연", 1, List.of(item));

        given(symptomReportService.getSymptomsByPatientId(eq(1L), eq(WARD_ID), eq(LocalDate.of(2026, 4, 29))))
                .willReturn(response);

        mockMvc.perform(get("/patients/1/symptoms").param("date", "2026-04-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientName").value("이승연"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.symptoms[0].buttonLabel").value("통증"));
    }

    @Test
    @DisplayName("GET /patients/{patientId}/symptoms?date= - 다른 병동 환자 → 403")
    void getSymptoms_실패_다른_병동() throws Exception {
        given(symptomReportService.getSymptomsByPatientId(eq(1L), eq(WARD_ID), any()))
                .willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD));

        mockMvc.perform(get("/patients/1/symptoms").param("date", "2026-04-29"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /patients/{patientId}/symptoms?date= - 환자 없으면 404")
    void getSymptoms_실패_환자_없음() throws Exception {
        given(symptomReportService.getSymptomsByPatientId(eq(99L), eq(WARD_ID), any()))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(get("/patients/99/symptoms").param("date", "2026-04-29"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /patients/{patientId}/symptoms?date= - 활성 입원 없으면 404")
    void getSymptoms_실패_입원_없음() throws Exception {
        given(symptomReportService.getSymptomsByPatientId(eq(1L), eq(WARD_ID), any()))
                .willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        mockMvc.perform(get("/patients/1/symptoms").param("date", "2026-04-29"))
                .andExpect(status().isNotFound());
    }
}