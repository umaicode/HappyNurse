package com.ssafy.happynurse.domain.nurse.notification.controller;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
