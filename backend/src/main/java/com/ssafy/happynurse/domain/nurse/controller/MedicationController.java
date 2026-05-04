package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueRequest;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueResponse;
import com.ssafy.happynurse.domain.nfc.service.NfcTagIssueService;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyResponse;
import com.ssafy.happynurse.domain.nurse.service.MedicationService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "투약", description = "NFC 기반 약물 투약 관련 API")
@RestController
@RequestMapping("/drug")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;
    private final NfcTagIssueService nfcTagIssueService;

    @Operation(summary = "약물-환자 NFC 2중 확인",
            description = "환자 팔찌 태깅 후 스캔한 약물 NFC UID 목록을 환자 처방과 대조하여 검증합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패 (해당 환자의 처방 아님 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NFC 태그/환자 없음")
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<MedicationVerifyResponse>> verify(
            @Valid @RequestBody MedicationVerifyRequest request
    ) {
        MedicationVerifyResponse data = medicationService.verify(request);
        return ResponseEntity.ok(ApiResponse.ok("약물-환자 검증에 성공했습니다.", data));
    }

    @Operation(summary = "약물 투약 기록 저장",
            description = "검증 완료된 처방 ID 배열을 받아 처방 단위로 투약 기록을 일괄 저장하고, 처방 상태를 completed 로 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "환자/처방 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자/입원/의료진/처방 없음")
    })
    @PostMapping("/record")
    public ResponseEntity<ApiResponse<MedicationAdministrationSaveResponse>> saveAdministrations(
            @Valid @RequestBody MedicationAdministrationSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationAdministrationSaveResponse data = medicationService.saveAdministrations(
                request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("투약 기록을 저장했습니다.", data));
    }

    @Operation(summary = "약물 NFC 태그 발급",
            description = "빈 NFC 칩 시리얼(tagUid)에 의미(payload=ORDER|DRUG, id)를 등록한다. (권한: 간호사)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "payload 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "참조 처방/약물 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 발급된 tagUid")
    })
    @PostMapping("/tags")
    public ResponseEntity<ApiResponse<NfcTagIssueResponse>> issueTag(
            @Valid @RequestBody NfcTagIssueRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NfcTagIssueResponse data = nfcTagIssueService.issue(request, userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.ok("NFC 태그를 발급했습니다.", data));
    }
}
