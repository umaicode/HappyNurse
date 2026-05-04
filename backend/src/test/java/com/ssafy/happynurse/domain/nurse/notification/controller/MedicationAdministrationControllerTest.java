package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationWriteResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationItemResponse;
import com.ssafy.happynurse.domain.nurse.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.service.MedicationAdministrationService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MedicationAdministrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class MedicationAdministrationControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    MedicationAdministrationService medicationAdministrationService;

    private static final String TAG = "tag-uuid-1";

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
    @DisplayName("POST /medication-administrations/tagging/{tag}/confirm - 200")
    void confirm_성공_200() throws Exception {
        MedicationAdministrationWriteResponse response = new MedicationAdministrationWriteResponse(
                TAG, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 14, 25),
                List.of(new MedicationItemResponse(31L, "PC1", "약A",
                        new BigDecimal("1.000"), "mg", null, null)));
        given(medicationAdministrationService.confirm(eq(TAG), eq(1L))).willReturn(response);

        mockMvc.perform(post("/medication-administrations/tagging/" + TAG + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taggingId").value(TAG))
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.data.medications[0].productCode").value("PC1"));
    }

    @Test
    @DisplayName("POST confirm - draft 아님 400")
    void confirm_실패_status_400() throws Exception {
        given(medicationAdministrationService.confirm(eq(TAG), any()))
                .willThrow(new CustomException(ErrorCode.INVALID_RECORD_STATUS));

        mockMvc.perform(post("/medication-administrations/tagging/" + TAG + "/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECORD_STATUS"));
    }

    @Test
    @DisplayName("POST confirm - 그룹 없음 404")
    void confirm_실패_없음_404() throws Exception {
        given(medicationAdministrationService.confirm(eq(TAG), any()))
                .willThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_FOUND));

        mockMvc.perform(post("/medication-administrations/tagging/" + TAG + "/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH - 200 (effectiveDatetime + medications)")
    void update_성공_200() throws Exception {
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 3, 16, 0);
        MedicationAdministrationWriteResponse response = new MedicationAdministrationWriteResponse(
                TAG, RecordStatus.draft, newTime,
                List.of(new MedicationItemResponse(31L, "PC1", "약A",
                        new BigDecimal("1.500"), "tab", null, null)));
        given(medicationAdministrationService.update(eq(TAG), any(), eq(1L))).willReturn(response);

        String body = "{\"effectiveDatetime\":\"2026-05-03T16:00:00\","
                + "\"medications\":[{\"medicationAdminId\":31,\"dosageQuantity\":1.500,\"dosageUnit\":\"tab\"}]}";

        mockMvc.perform(patch("/medication-administrations/tagging/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveDatetime").value("2026-05-03T16:00:00"))
                .andExpect(jsonPath("$.data.medications[0].dosageQuantity").value(1.500));
    }

    @Test
    @DisplayName("PATCH - 그룹 외 id 400")
    void update_실패_그룹_외_400() throws Exception {
        willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE))
                .given(medicationAdministrationService).update(eq(TAG), any(), any());

        String body = "{\"medications\":[{\"medicationAdminId\":999,\"dosageQuantity\":1.0,\"dosageUnit\":\"mg\"}]}";

        mockMvc.perform(patch("/medication-administrations/tagging/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE - 200")
    void delete_성공_200() throws Exception {
        mockMvc.perform(delete("/medication-administrations/tagging/" + TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("투약 기록을 삭제했습니다."));
    }

    @Test
    @DisplayName("DELETE - 타작성자 403")
    void delete_실패_타작성자_403() throws Exception {
        doThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR))
                .when(medicationAdministrationService).delete(eq(TAG), any());

        mockMvc.perform(delete("/medication-administrations/tagging/" + TAG))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_AUTHOR"));
    }
}
