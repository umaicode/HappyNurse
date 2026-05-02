package com.ssafy.happynurse.domain.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.webapp.dto.DevVerifyRequest;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyResult;
import com.ssafy.happynurse.domain.webapp.service.WebappService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DevWebappController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "app.dev.enabled=true",
        "jwt.cookie-name=ACCESS_TOKEN",
        "jwt.access-token-expiration-ms=1800000",
        "jwt.cookie-secure=false",
        "jwt.cookie-same-site=Lax"
})
class DevWebappControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean WebappService webappService;

    private static final PatientVerifyResult VERIFY_RESULT = new PatientVerifyResult(
            "mock-patient-token", 1L, "김가민", "301호실",
            "female", "정형외과", "퇴행성 무릎 관절염", "무릎 통증",
            "슬관절 전치환술", "문현지"
    );

    @Test
    @DisplayName("POST /patients/dev-verify 성공 — accessToken + 환자 정보 반환, Cookie 설정")
    void devVerify_성공() throws Exception {
        given(webappService.devVerify(1L)).willReturn(VERIFY_RESULT);

        mockMvc.perform(post("/patients/dev-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DevVerifyRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-patient-token"))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.patientName").value("김가민"))
                .andExpect(jsonPath("$.data.roomName").value("301호실"))
                .andExpect(jsonPath("$.data.diseaseName").value("퇴행성 무릎 관절염"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /patients/dev-verify — 존재하지 않는 환자 → 404 PATIENT_NOT_FOUND")
    void devVerify_환자없음() throws Exception {
        given(webappService.devVerify(99L))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(post("/patients/dev-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DevVerifyRequest(99L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PATIENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /patients/dev-verify — patientId null → 400")
    void devVerify_입력값오류() throws Exception {
        mockMvc.perform(post("/patients/dev-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}