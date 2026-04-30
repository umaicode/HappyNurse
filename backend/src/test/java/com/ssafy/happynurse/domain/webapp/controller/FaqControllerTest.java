package com.ssafy.happynurse.domain.webapp.controller;

import com.ssafy.happynurse.domain.webapp.dto.FaqItemResponse;
import com.ssafy.happynurse.domain.webapp.dto.FaqListResponse;
import com.ssafy.happynurse.domain.webapp.service.FaqService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FaqController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FaqControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqService faqService;

    @BeforeEach
    void setUpSecurity() {
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
    @DisplayName("GET /patients/{id}/faq - 정상 응답")
    void getFaq_success() throws Exception {
        FaqListResponse mockResponse = new FaqListResponse(
                "퇴행성 무릎 관절염",
                "퇴행성 관절염",
                List.of(
                        new FaqItemResponse("재활", "재활은 어떻게 진행되나요?", "재활 운동은 ..."),
                        new FaqItemResponse("정의", "이 질환은 어떤 병인가요?", "퇴행성 관절염은 ...")
                ));
        given(faqService.getFaq(1L, 1L)).willReturn(mockResponse);

        mockMvc.perform(get("/patients/1/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diseaseName").value("퇴행성 무릎 관절염"))
                .andExpect(jsonPath("$.data.matchedFaqDisease").value("퇴행성 관절염"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].intentLabel").value("재활"))
                .andExpect(jsonPath("$.data.items[0].question").value("재활은 어떻게 진행되나요?"))
                .andExpect(jsonPath("$.data.items[0].answer").value("재활 운동은 ..."))
                .andExpect(jsonPath("$.data.items[1].intentLabel").value("정의"));
    }

    @Test
    @DisplayName("GET /patients/{id}/faq - 매칭 실패 시 빈 items")
    void getFaq_noMatch() throws Exception {
        FaqListResponse mockResponse = new FaqListResponse(
                "복부 비만", null, List.of());
        given(faqService.getFaq(1L, 1L)).willReturn(mockResponse);

        mockMvc.perform(get("/patients/1/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchedFaqDisease").doesNotExist())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("GET /patients/{id}/faq - JWT patientId ≠ path patientId → 403")
    void getFaq_patientIdMismatch() throws Exception {
        given(faqService.getFaq(1L, 99L))
                .willThrow(new CustomException(ErrorCode.PATIENT_ID_MISMATCH));

        mockMvc.perform(get("/patients/99/faq"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /patients/{id}/faq - 활성 입원 없음 → 404")
    void getFaq_encounterNotFound() throws Exception {
        given(faqService.getFaq(1L, 1L))
                .willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        mockMvc.perform(get("/patients/1/faq"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /patients/{id}/faq - 환자 없음 → 404")
    void getFaq_patientNotFound() throws Exception {
        given(faqService.getFaq(1L, 1L))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(get("/patients/1/faq"))
                .andExpect(status().isNotFound());
    }
}