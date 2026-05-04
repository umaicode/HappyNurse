package com.ssafy.happynurse.domain.nurse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueRequest;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueResponse;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.service.NfcTagIssueService;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyResponse;
import com.ssafy.happynurse.domain.nurse.service.MedicationService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MedicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class MedicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MedicationService medicationService;
    @MockitoBean NfcTagIssueService nfcTagIssueService;

    @BeforeEach
    void setUpSecurity() {
        authAs("nurse");
    }

    private void authAs(String role) {
        CustomUserDetails userDetails = new CustomUserDetails(
                7L, "EMP-007", "김간호", role, "session-1", 1L, 3L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /drug/verify - 단일 태그 검증 성공")
    void verify_성공() throws Exception {
        MedicationVerifyRequest request = new MedicationVerifyRequest(3L, "UID-DRUG-001");
        given(medicationService.verify(any(MedicationVerifyRequest.class)))
                .willReturn(MedicationVerifyResponse.success(12345L));

        mockMvc.perform(post("/drug/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.medicationOrderId").value(12345));
    }

    @Test
    @DisplayName("POST /drug/verify - 검증 실패시 400 + 에러코드")
    void verify_실패_환자불일치() throws Exception {
        MedicationVerifyRequest request = new MedicationVerifyRequest(3L, "UID-X");
        given(medicationService.verify(any(MedicationVerifyRequest.class)))
                .willThrow(new CustomException(ErrorCode.MEDICATION_VERIFICATION_FAILED));

        mockMvc.perform(post("/drug/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_VERIFICATION_FAILED"));
    }

    @Test
    @DisplayName("POST /drug/verify - 약물 태그가 아니면 400 NFC_TAG_NOT_MEDICATION")
    void verify_실패_wristband_태그() throws Exception {
        MedicationVerifyRequest request = new MedicationVerifyRequest(3L, "UID-PATIENT-001");
        given(medicationService.verify(any(MedicationVerifyRequest.class)))
                .willThrow(new CustomException(ErrorCode.NFC_TAG_NOT_MEDICATION));

        mockMvc.perform(post("/drug/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NFC_TAG_NOT_MEDICATION"));
    }

    @Test
    @DisplayName("POST /drug/verify - tagUid 누락 시 400")
    void verify_validation_실패() throws Exception {
        String body = """
                {"patientId": 3, "tagUid": ""}
                """;

        mockMvc.perform(post("/drug/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /drug/record - 저장 성공 (practitionerId 는 인증 컨텍스트에서)")
    void administrations_성공() throws Exception {
        MedicationAdministrationSaveRequest request =
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L, 12346L, 12347L));
        given(medicationService.saveAdministrations(any(), eq(7L)))
                .willReturn(new MedicationAdministrationSaveResponse(
                        "8f3c8b1e-9b1d-4f8c-a8d3-2b7c1e6e4a90", 3, List.of(101L, 102L, 103L)));

        mockMvc.perform(post("/drug/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taggingId").value("8f3c8b1e-9b1d-4f8c-a8d3-2b7c1e6e4a90"))
                .andExpect(jsonPath("$.data.savedCount").value(3))
                .andExpect(jsonPath("$.data.medicationAdminIds[0]").value(101))
                .andExpect(jsonPath("$.data.medicationAdminIds[2]").value(103));
    }

    @Test
    @DisplayName("POST /drug/record - 환자 없으면 404")
    void administrations_환자없음() throws Exception {
        MedicationAdministrationSaveRequest request =
                new MedicationAdministrationSaveRequest(99L, 11L, List.of(12345L));
        given(medicationService.saveAdministrations(any(), any()))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(post("/drug/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PATIENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /drug/record - 이미 투약 완료된 처방이면 409 MEDICATION_ALREADY_ADMINISTERED")
    void administrations_이미_완료() throws Exception {
        MedicationAdministrationSaveRequest request =
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L));
        given(medicationService.saveAdministrations(any(), any()))
                .willThrow(new CustomException(ErrorCode.MEDICATION_ALREADY_ADMINISTERED));

        mockMvc.perform(post("/drug/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_ALREADY_ADMINISTERED"));
    }

    @Test
    @DisplayName("POST /drug/record - medicationOrderIds 누락 시 400")
    void administrations_validation_실패() throws Exception {
        String body = """
                {"patientId": 3, "encounterId": 11, "medicationOrderIds": []}
                """;

        mockMvc.perform(post("/drug/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------- /drug/tags (NFC 발급) ----------

    @Test
    @DisplayName("POST /drug/tags - 발급 성공 (인증 principal 의 role 이 service 로 전달)")
    void issueTag_성공() throws Exception {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-DRUG-NEW", TagType.medication, "DRUG", 789L);
        given(nfcTagIssueService.issue(any(), eq("nurse")))
                .willReturn(new NfcTagIssueResponse(
                        42L, "UID-DRUG-NEW", TagType.medication,
                        new NfcPayload("DRUG", 789L), true,
                        LocalDateTime.of(2026, 5, 4, 14, 0)));

        mockMvc.perform(post("/drug/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nfcTagId").value(42))
                .andExpect(jsonPath("$.data.tagUid").value("UID-DRUG-NEW"))
                .andExpect(jsonPath("$.data.payload.type").value("DRUG"))
                .andExpect(jsonPath("$.data.payload.id").value(789))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /drug/tags - 권한 없는 role 이면 403")
    void issueTag_권한없음() throws Exception {
        authAs("doctor");
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-X", TagType.medication, "DRUG", 789L);
        given(nfcTagIssueService.issue(any(), eq("doctor")))
                .willThrow(new CustomException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/drug/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /drug/tags - 같은 시리얼 재사용 시 덮어쓰기(200), 같은 nfcTagId 반환")
    void issueTag_재발급_업서트() throws Exception {
        NfcTagIssueRequest req = new NfcTagIssueRequest("UID-REUSE", TagType.medication, "DRUG", 789L);
        // 서비스가 기존 row 의 PK 를 그대로 유지한 채 의미만 갈아끼웠다고 가정
        given(nfcTagIssueService.issue(any(), eq("nurse")))
                .willReturn(new NfcTagIssueResponse(
                        7L, "UID-REUSE", TagType.medication,
                        new NfcPayload("DRUG", 789L), true,
                        LocalDateTime.of(2026, 5, 4, 14, 30)));

        mockMvc.perform(post("/drug/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nfcTagId").value(7))
                .andExpect(jsonPath("$.data.tagUid").value("UID-REUSE"))
                .andExpect(jsonPath("$.data.payload.type").value("DRUG"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /drug/tags - payloadType 이 ORDER/DRUG 외 값이면 400 (validation)")
    void issueTag_payloadType_validation() throws Exception {
        String body = """
                {"tagUid":"UID-X","tagType":"medication","payloadType":"PATIENT","payloadId":1}
                """;

        mockMvc.perform(post("/drug/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /drug/tags - tagUid 누락 시 400")
    void issueTag_tagUid_validation() throws Exception {
        String body = """
                {"tagType":"medication","payloadType":"DRUG","payloadId":789}
                """;

        mockMvc.perform(post("/drug/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
