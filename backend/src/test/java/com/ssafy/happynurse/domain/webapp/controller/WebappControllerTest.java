package com.ssafy.happynurse.domain.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.webapp.dto.*;
import com.ssafy.happynurse.domain.webapp.service.WebappService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebappController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WebappControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    WebappService webappService;

    @BeforeEach
    void setUpSecurity() {
        // 환자 JWT로 로그인한 상태를 흉내냄 (patientId=1, role=PATIENT)
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, null, "김가민", "PATIENT", null, null, null);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/nfc/patients/{id}/entry - 정상 응답")
    void nfcEntry_success() throws Exception {
        // given
        given(webappService.getPatientEntry(1L))
                .willReturn(new NfcEntryResponse(1L, "김가민", "301호실"));

        // when & then
        mockMvc.perform(get("/api/nfc/patients/1/entry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientName").value("김가민"))
                .andExpect(jsonPath("$.data.roomName").value("301호실"));
    }

    @Test
    @DisplayName("GET /api/nfc/patients/{id}/entry - 환자 없으면 404")
    void nfcEntry_notFound() throws Exception {
        given(webappService.getPatientEntry(99L))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(get("/api/nfc/patients/99/entry"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/patients/verify - 성공 시 HttpOnly 쿠키 설정 및 환자 정보 반환")
    void verify_success_setCookie() throws Exception {
        // given
        PatientVerifyRequest request = new PatientVerifyRequest();
        request.setPatientId(1L);
        request.setName("김가민");
        request.setBirthDate("010429");

        given(webappService.verifyPatient(any()))
                .willReturn(new PatientVerifyResult(
                        "mock-token", 1L, "김가민", "301호실",
                        "female", "정형외과", "퇴행성 무릎 관절염",
                        "무릎 통증", null, "문현지"
                ));

        // when & then
        mockMvc.perform(post("/api/patients/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
                .andExpect(cookie().exists("ACCESS_TOKEN"))
                .andExpect(jsonPath("$.data.patientName").value("김가민"))
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.departmentCode").value("정형외과"))
                .andExpect(jsonPath("$.data.diseaseName").value("퇴행성 무릎 관절염"))
                .andExpect(jsonPath("$.data.chiefComplaint").value("무릎 통증"))
                .andExpect(jsonPath("$.data.surgeryName").doesNotExist())
                .andExpect(jsonPath("$.data.assignedNurseName").value("문현지"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/patients/verify - 검증 실패 시 401")
    void verify_failed() throws Exception {
        // given
        PatientVerifyRequest request = new PatientVerifyRequest();
        request.setPatientId(1L);
        request.setName("이다른");
        request.setBirthDate("010429");

        given(webappService.verifyPatient(any()))
                .willThrow(new CustomException(ErrorCode.PATIENT_VERIFY_FAILED));

        mockMvc.perform(post("/api/patients/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/patients/verify - birthDate 형식 오류 시 400")
    void verify_invalidBirthDate() throws Exception {
        // given
        PatientVerifyRequest request = new PatientVerifyRequest();
        request.setPatientId(1L);
        request.setName("김가민");
        request.setBirthDate("abc");

        mockMvc.perform(post("/api/patients/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/symptoms/buttons - 버튼 목록 정상 반환")
    void getButtons_success() throws Exception {
        given(webappService.getButtons())
                .willReturn(List.of(
                        new SymptomButtonResponse(1L, "드레싱 교체", "상처 부위 드레싱이 필요합니다", 1),
                        new SymptomButtonResponse(2L, "통증", "통증이 있습니다", 2)
                ));

        mockMvc.perform(get("/api/symptoms/buttons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("드레싱 교체"))
                .andExpect(jsonPath("$.data[1].label").value("통증"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/symptoms - 버튼 선택 성공")
    void submitSymptom_button_success() throws Exception {
        // Mockito 규칙: 인자 중 하나라도 matcher를 쓰면 모든 인자에 matcher 사용 필요
        given(webappService.submitSymptom(any(), any(), any()))
                .willReturn(new SymptomSubmitResponse(42L, LocalDateTime.now()));

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(1L);

        mockMvc.perform(post("/api/patients/1/symptoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selfReportId").value(42));
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/symptoms - 다른 환자 ID 시도 -> 403")
    void submitSymptom_patientIdMismatch() throws Exception {
        given(webappService.submitSymptom(any(), any(), any()))
                .willThrow(new CustomException(ErrorCode.PATIENT_ID_MISMATCH));

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(1L);

        mockMvc.perform(post("/api/patients/99/symptoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
