package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.PractitionerMeResponse;
import com.ssafy.happynurse.domain.auth.service.PractitionerService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PractitionerController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PractitionerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PractitionerService practitionerService;

    private static final PractitionerMeResponse ME_RESPONSE = new PractitionerMeResponse(
            1L, "홍길동", "EMP001", "nurse", 3L, "내과 3병동");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /practitioners/me 성공 시 200 + 본인 정보 반환, 토큰의 practitionerId/wardId가 service에 전달된다")
    void getMyInfo_성공() throws Exception {
        authenticate(new CustomUserDetails(1L, "EMP001", "홍길동", "nurse", "session-1", 1L, 3L));
        given(practitionerService.getMyInfo(anyLong(), anyLong())).willReturn(ME_RESPONSE);

        mockMvc.perform(get("/practitioners/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.practitionerId").value(1))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.data.roleCode").value("nurse"))
                .andExpect(jsonPath("$.data.wardId").value(3))
                .andExpect(jsonPath("$.data.wardName").value("내과 3병동"));

        ArgumentCaptor<Long> practitionerIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> wardIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(practitionerService).getMyInfo(practitionerIdCaptor.capture(), wardIdCaptor.capture());
        assertThat(practitionerIdCaptor.getValue()).isEqualTo(1L);
        assertThat(wardIdCaptor.getValue()).isEqualTo(3L);
    }

    @Test
    @DisplayName("GET /practitioners/me - Practitioner 없음 시 404 + PRACTITIONER_NOT_FOUND")
    void getMyInfo_실패_Practitioner_없음() throws Exception {
        authenticate(new CustomUserDetails(99L, "EMP999", "유령", "nurse", "session-1", 1L, 3L));
        given(practitionerService.getMyInfo(anyLong(), anyLong()))
                .willThrow(new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        mockMvc.perform(get("/practitioners/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PRACTITIONER_NOT_FOUND"));
    }

    private void authenticate(CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}