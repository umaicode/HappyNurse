package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.dto.MedicationItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemType;
import com.ssafy.happynurse.domain.nurse.service.NursingNoteEditService;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
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

@WebMvcTest(controllers = NursingNoteEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class NursingNoteEditControllerTest {

    private static final String TAG = "8b2a3f6c-7d4e-4a1b-9c2d-1e2f3a4b5c6d";

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    NursingNoteEditService nursingNoteEditService;

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
    @DisplayName("PATCH /nursing-notes/stt/{id} - 200 성공")
    void updateStt_성공_200() throws Exception {
        NursingNoteItemResponse response = new NursingNoteItemResponse(
                NursingNoteItemType.STT_NOTE,
                LocalDateTime.of(2026, 5, 3, 11, 30),
                RecordStatus.amended,
                1L, "김간호", true,
                12L, "수정된 본문",
                null, null, null);
        given(nursingNoteEditService.updateSttNote(eq(12L), any(), eq(1L))).willReturn(response);

        mockMvc.perform(patch("/nursing-notes/stt/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("STT_NOTE"))
                .andExpect(jsonPath("$.data.nursingRecordId").value(12))
                .andExpect(jsonPath("$.data.content").value("수정된 본문"))
                .andExpect(jsonPath("$.data.status").value("amended"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/stt/{id} - nursingRecordId 형식 오류 400")
    void updateStt_실패_id_형식_400() throws Exception {
        mockMvc.perform(patch("/nursing-notes/stt/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/stt/{id} - 빈 본문 400")
    void updateStt_실패_빈본문_400() throws Exception {
        willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE))
                .given(nursingNoteEditService).updateSttNote(eq(12L), any(), any());

        mockMvc.perform(patch("/nursing-notes/stt/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /nursing-notes/stt/{id} - 타작성자 403")
    void updateStt_실패_타작성자_403() throws Exception {
        willThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR))
                .given(nursingNoteEditService).updateSttNote(eq(12L), any(), any());

        mockMvc.perform(patch("/nursing-notes/stt/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_AUTHOR"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/stt/{id} - 기록 없음 404")
    void updateStt_실패_없음_404() throws Exception {
        willThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND))
                .given(nursingNoteEditService).updateSttNote(eq(99L), any(), any());

        mockMvc.perform(patch("/nursing-notes/stt/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/medication/{tag} - 200 성공 (medications)")
    void updateMed_성공_200() throws Exception {
        NursingNoteItemResponse response = new NursingNoteItemResponse(
                NursingNoteItemType.MEDICATION,
                LocalDateTime.of(2026, 5, 3, 16, 0),
                RecordStatus.draft,
                1L, "김간호", true,
                null, null,
                TAG, true,
                List.of(new MedicationItemResponse(31L, "PC1", "약A",
                        new BigDecimal("1.500"), "tab", null, null, null)));
        given(nursingNoteEditService.updateMedication(eq(TAG), any(), eq(1L))).willReturn(response);

        String body = "{\"medications\":[{\"medicationAdminId\":31,\"dosageQuantity\":1.500}]}";

        mockMvc.perform(patch("/nursing-notes/medication/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("MEDICATION"))
                .andExpect(jsonPath("$.data.taggingId").value(TAG))
                .andExpect(jsonPath("$.data.medications[0].dosageQuantity").value(1.500));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/medication/{tag} - 200 성공 (confirmedAt 만)")
    void updateMed_성공_시각만_200() throws Exception {
        NursingNoteItemResponse response = new NursingNoteItemResponse(
                NursingNoteItemType.MEDICATION,
                LocalDateTime.of(2026, 5, 4, 10, 0),
                RecordStatus.confirmed,
                1L, "김간호", true,
                null, null,
                TAG, true,
                List.of(new MedicationItemResponse(31L, "PC1", "약A",
                        new BigDecimal("1.000"), "tab", null, null, null)));
        given(nursingNoteEditService.updateMedication(eq(TAG), any(), eq(1L))).willReturn(response);

        String body = "{\"confirmedAt\":\"2026-05-04T10:00:00\"}";

        mockMvc.perform(patch("/nursing-notes/medication/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurredAt").value("2026-05-04T10:00:00"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/medication/{tag} - 그룹 외 id → MEDICATION_ADMIN_NOT_IN_GROUP 400")
    void updateMed_실패_그룹외_400() throws Exception {
        willThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_IN_GROUP))
                .given(nursingNoteEditService).updateMedication(eq(TAG), any(), any());

        String body = "{\"medications\":[{\"medicationAdminId\":999,\"dosageQuantity\":1.0}]}";

        mockMvc.perform(patch("/nursing-notes/medication/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_IN_GROUP"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/medication/{tag} - 타작성자 403")
    void updateMed_실패_타작성자_403() throws Exception {
        willThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR))
                .given(nursingNoteEditService).updateMedication(eq(TAG), any(), any());

        String body = "{\"medications\":[{\"medicationAdminId\":31,\"dosageQuantity\":1.0}]}";

        mockMvc.perform(patch("/nursing-notes/medication/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_AUTHOR"));
    }

    @Test
    @DisplayName("PATCH /nursing-notes/medication/{tag} - 그룹 없음 404")
    void updateMed_실패_없음_404() throws Exception {
        willThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_FOUND))
                .given(nursingNoteEditService).updateMedication(eq(TAG), any(), any());

        String body = "{\"medications\":[{\"medicationAdminId\":31,\"dosageQuantity\":1.0}]}";

        mockMvc.perform(patch("/nursing-notes/medication/" + TAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /nursing-notes/{id}/confirm - 숫자 itemId → STT 분기 200")
    void confirm_STT_성공_200() throws Exception {
        NursingNoteItemResponse response = new NursingNoteItemResponse(
                NursingNoteItemType.STT_NOTE,
                LocalDateTime.of(2026, 5, 3, 14, 30),
                RecordStatus.confirmed,
                1L, "김간호", true,
                12L, "녹음 본문",
                null, null, null);
        given(nursingNoteEditService.confirm(eq("12"), eq(1L))).willReturn(response);

        mockMvc.perform(post("/nursing-notes/12/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("STT_NOTE"))
                .andExpect(jsonPath("$.data.nursingRecordId").value(12))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    @DisplayName("POST /nursing-notes/{id}/confirm - UUID itemId → MEDICATION 분기 200")
    void confirm_MED_성공_200() throws Exception {
        NursingNoteItemResponse response = new NursingNoteItemResponse(
                NursingNoteItemType.MEDICATION,
                LocalDateTime.of(2026, 5, 3, 14, 25),
                RecordStatus.confirmed,
                1L, "김간호", true,
                null, null,
                TAG, true,
                List.of(new MedicationItemResponse(31L, "PC1", "약A",
                        new BigDecimal("1.000"), "tab", null, null, null)));
        given(nursingNoteEditService.confirm(eq(TAG), eq(1L))).willReturn(response);

        mockMvc.perform(post("/nursing-notes/" + TAG + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("MEDICATION"))
                .andExpect(jsonPath("$.data.taggingId").value(TAG))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    @DisplayName("POST /nursing-notes/{id}/confirm - draft 아님 400")
    void confirm_실패_status_400() throws Exception {
        given(nursingNoteEditService.confirm(eq("12"), any()))
                .willThrow(new CustomException(ErrorCode.INVALID_RECORD_STATUS));

        mockMvc.perform(post("/nursing-notes/12/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECORD_STATUS"));
    }

    @Test
    @DisplayName("POST /nursing-notes/{id}/confirm - 타작성자 403")
    void confirm_실패_타작성자_403() throws Exception {
        given(nursingNoteEditService.confirm(eq("12"), any()))
                .willThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR));

        mockMvc.perform(post("/nursing-notes/12/confirm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_AUTHOR"));
    }

    @Test
    @DisplayName("POST /nursing-notes/{id}/confirm - 없음 404")
    void confirm_실패_없음_404() throws Exception {
        given(nursingNoteEditService.confirm(eq(TAG), any()))
                .willThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_FOUND));

        mockMvc.perform(post("/nursing-notes/" + TAG + "/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /nursing-notes/{id} - 숫자 itemId → STT 분기 200")
    void delete_STT_성공_200() throws Exception {
        mockMvc.perform(delete("/nursing-notes/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("간호 기록을 삭제했습니다."));
    }

    @Test
    @DisplayName("DELETE /nursing-notes/{id} - UUID itemId → MEDICATION 분기 200")
    void delete_MED_성공_200() throws Exception {
        mockMvc.perform(delete("/nursing-notes/" + TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("간호 기록을 삭제했습니다."));
    }

    @Test
    @DisplayName("DELETE /nursing-notes/{id} - 타작성자 403")
    void delete_실패_타작성자_403() throws Exception {
        doThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR))
                .when(nursingNoteEditService).delete(eq("12"), any());

        mockMvc.perform(delete("/nursing-notes/12"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NURSING_RECORD_NOT_AUTHOR"));
    }

    @Test
    @DisplayName("DELETE /nursing-notes/{id} - 없음 404")
    void delete_실패_없음_404() throws Exception {
        doThrow(new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_FOUND))
                .when(nursingNoteEditService).delete(eq(TAG), any());

        mockMvc.perform(delete("/nursing-notes/" + TAG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ADMIN_NOT_FOUND"));
    }
}