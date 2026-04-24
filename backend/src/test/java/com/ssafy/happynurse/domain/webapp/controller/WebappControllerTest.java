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
    @DisplayName("POST /api/patients/verify - 성공 시 HttpOnly 쿠키 설정")
    void verify_success_setCookie() throws Exception {
        // given
        PatientVerifyRequest request = new PatientVerifyRequest();
        request.setPatientId(1L);
        request.setName("김가민");
        request.setBirthDate("010429");

        given(webappService.verifyPatient(any()))
                .willReturn(new PatientVerifyResult("mock-token", 1L, "김가민", "301호실"));

        // when & then
        mockMvc.perform(post("/api/patients/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
                .andExpect(cookie().exists("ACCESS_TOKEN"))
                .andExpect(jsonPath("$.data.patientName").value("김가민"))
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
}
