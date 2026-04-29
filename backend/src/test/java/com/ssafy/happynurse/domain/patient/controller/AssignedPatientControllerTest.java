package com.ssafy.happynurse.domain.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateRequest;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateResponse;
import com.ssafy.happynurse.domain.patient.service.AssignedPatientService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AssignedPatientController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AssignedPatientControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AssignedPatientService assignedPatientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PUT /practitioners/me/patients 성공 시 200 + 결과 DTO 반환")
    void update_성공() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP099", "본인", "nurse", "session-1", 1L, 3L));
        given(assignedPatientService.updateMyAssignedPatients(anyLong(), anyLong(), any()))
                .willReturn(new AssignedPatientUpdateResponse(
                        List.of(100L, 101L), List.of(203L), 1));

        mockMvc.perform(put("/practitioners/me/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignedPatientUpdateRequest(List.of(100L, 101L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("담당 환자 저장에 성공했습니다."))
                .andExpect(jsonPath("$.data.assignedEncounterIds[0]").value(100))
                .andExpect(jsonPath("$.data.assignedEncounterIds[1]").value(101))
                .andExpect(jsonPath("$.data.releasedEncounterIds[0]").value(203))
                .andExpect(jsonPath("$.data.overwroteFromOthersCount").value(1));
    }

    @Test
    @DisplayName("PUT /practitioners/me/patients 빈 배열도 200 처리")
    void update_빈배열_성공() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP099", "본인", "nurse", "session-1", 1L, 3L));
        given(assignedPatientService.updateMyAssignedPatients(anyLong(), anyLong(), any()))
                .willReturn(new AssignedPatientUpdateResponse(List.of(), List.of(), 0));

        mockMvc.perform(put("/practitioners/me/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"encounterIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedEncounterIds").isArray())
                .andExpect(jsonPath("$.data.releasedEncounterIds").isArray());
    }

    @Test
    @DisplayName("PUT /practitioners/me/patients - encounterIds null 이면 400")
    void update_null_바디_400() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP099", "본인", "nurse", "session-1", 1L, 3L));

        mockMvc.perform(put("/practitioners/me/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /practitioners/me/patients - 다른 병동 ID 섞이면 403")
    void update_다른병동_ID_403() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP099", "본인", "nurse", "session-1", 1L, 3L));
        willThrow(new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD))
                .given(assignedPatientService).updateMyAssignedPatients(anyLong(), anyLong(), any());

        mockMvc.perform(put("/practitioners/me/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"encounterIds\":[999]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void authenticate(CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
