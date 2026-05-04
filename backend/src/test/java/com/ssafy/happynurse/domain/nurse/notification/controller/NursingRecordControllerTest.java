package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.dto.NursingRecordWriteResponse;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.service.NursingRecordService;
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

import java.time.LocalDateTime;

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

@WebMvcTest(controllers = NursingRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class NursingRecordControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    NursingRecordService nursingRecordService;

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
    @DisplayName("POST /nursing-records/{id}/confirm - 200 성공")
    void confirm_성공_200() throws Exception {
        NursingRecordWriteResponse response = new NursingRecordWriteResponse(
                12L, RecordStatus.confirmed, "녹음 본문",
                LocalDateTime.of(2026, 5, 3, 14, 30));
        given(nursingRecordService.confirm(eq(12L), eq(1L))).willReturn(response);

        mockMvc.perform(post("/nursing-records/12/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nursingRecordId").value(12))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    @DisplayName("POST /nursing-records/{id}/confirm - draft 아님 400")
    void confirm_실패_status_400() throws Exception {
        given(nursingRecordService.confirm(eq(12L), any()))
                .willThrow(new CustomException(ErrorCode.INVALID_RECORD_STATUS));

        mockMvc.perform(post("/nursing-records/12/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECORD_STATUS"));
    }

    @Test
    @DisplayName("POST /nursing-records/{id}/confirm - 타작성자 403")
    void confirm_실패_타작성자_403() throws Exception {
        given(nursingRecordService.confirm(eq(12L), any()))
                .willThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR));

        mockMvc.perform(post("/nursing-records/12/confirm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_AUTHOR"));
    }

    @Test
    @DisplayName("PATCH /nursing-records/{id} - 200 성공")
    void update_성공_200() throws Exception {
        NursingRecordWriteResponse response = new NursingRecordWriteResponse(
                12L, RecordStatus.amended, "수정된 본문",
                LocalDateTime.of(2026, 5, 3, 14, 30));
        given(nursingRecordService.update(eq(12L), any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/nursing-records/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("amended"))
                .andExpect(jsonPath("$.data.content").value("수정된 본문"));
    }

    @Test
    @DisplayName("PATCH /nursing-records/{id} - 빈 본문 400")
    void update_실패_빈본문_400() throws Exception {
        willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE))
                .given(nursingRecordService).update(eq(12L), any(), any());

        mockMvc.perform(patch("/nursing-records/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /nursing-records/{id} - 기록 없음 404")
    void update_실패_없음_404() throws Exception {
        willThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND))
                .given(nursingRecordService).update(eq(99L), any(), any());

        mockMvc.perform(patch("/nursing-records/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /nursing-records/{id} - 200 성공")
    void delete_성공_200() throws Exception {
        mockMvc.perform(delete("/nursing-records/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("간호 기록을 삭제했습니다."));
    }

    @Test
    @DisplayName("DELETE /nursing-records/{id} - 타작성자 403")
    void delete_실패_타작성자_403() throws Exception {
        doThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR))
                .when(nursingRecordService).delete(eq(12L), any());

        mockMvc.perform(delete("/nursing-records/12"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_AUTHOR"));
    }
}
