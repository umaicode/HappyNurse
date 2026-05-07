package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.WardPatientListResponse;
import com.ssafy.happynurse.domain.patient.entity.Gender;
import com.ssafy.happynurse.domain.patient.service.WardPatientListService;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WardPatientListController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WardPatientListControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WardPatientListService wardPatientListService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /wards/me/patients 성공 시 200 + 13 필드(담당 간호사·주증상·수술명 포함) 환자 목록 반환")
    void listMyWardPatients_성공() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP099", "본인", "nurse", "session-1", 1L, 3L));
        given(wardPatientListService.listWardPatients(anyLong(), anyLong()))
                .willReturn(List.of(
                        new WardPatientListResponse(1L, 100L, "김가민", Gender.female,
                                LocalDate.of(1999, 5, 20), "7101호", "A", 2L, true,
                                12L, "이수정", "복통", "충수절제술")
                ));

        mockMvc.perform(get("/wards/me/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("병동 환자 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data[0].patientId").value(1))
                .andExpect(jsonPath("$.data[0].encounterId").value(100))
                .andExpect(jsonPath("$.data[0].name").value("김가민"))
                .andExpect(jsonPath("$.data[0].gender").value("female"))
                .andExpect(jsonPath("$.data[0].birthDate").value("1999-05-20"))
                .andExpect(jsonPath("$.data[0].roomName").value("7101호"))
                .andExpect(jsonPath("$.data[0].bedName").value("A"))
                .andExpect(jsonPath("$.data[0].unconfirmedNursingCount").value(2))
                .andExpect(jsonPath("$.data[0].isMyPatient").value(true))
                .andExpect(jsonPath("$.data[0].assignedNurseId").value(12))
                .andExpect(jsonPath("$.data[0].assignedNurseName").value("이수정"))
                .andExpect(jsonPath("$.data[0].chiefComplaint").value("복통"))
                .andExpect(jsonPath("$.data[0].surgeryName").value("충수절제술"));

        ArgumentCaptor<Long> wardCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> meCaptor = ArgumentCaptor.forClass(Long.class);
        verify(wardPatientListService).listWardPatients(wardCaptor.capture(), meCaptor.capture());
        assertThat(wardCaptor.getValue()).isEqualTo(3L);
        assertThat(meCaptor.getValue()).isEqualTo(99L);
    }

    @Test
    @DisplayName("GET /wards/me/patients - 토큰의 wardId/practitionerId가 서비스에 그대로 전달된다")
    void listMyWardPatients_토큰_컨텍스트_전달() throws Exception {
        authenticate(new CustomUserDetails(7L, "EMP007", "다른사람", "nurse", "session-2", 2L, 5L));
        given(wardPatientListService.listWardPatients(anyLong(), anyLong()))
                .willReturn(List.of());

        mockMvc.perform(get("/wards/me/patients"))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> wardCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> meCaptor = ArgumentCaptor.forClass(Long.class);
        verify(wardPatientListService).listWardPatients(wardCaptor.capture(), meCaptor.capture());
        assertThat(wardCaptor.getValue()).isEqualTo(5L);
        assertThat(meCaptor.getValue()).isEqualTo(7L);
    }

    private void authenticate(CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
