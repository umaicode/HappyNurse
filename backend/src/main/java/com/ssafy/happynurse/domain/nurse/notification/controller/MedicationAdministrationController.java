package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationUpdateRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationWriteResponse;
import com.ssafy.happynurse.domain.nurse.service.MedicationAdministrationService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "투약 기록", description = "투약 기록(NFC 태깅 그룹) 확정·수정·삭제 API")
@RestController
@RequestMapping("/medication-administrations")
@RequiredArgsConstructor
public class MedicationAdministrationController {

    private final MedicationAdministrationService medicationAdministrationService;

    @Operation(summary = "투약 그룹 확정",
            description = "draft 상태의 taggingId 그룹을 confirmed로 일괄 전환. effectiveDatetime은 변경 안 함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "draft 아님 — INVALID_RECORD_STATUS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 투약 아님 — MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 없음 — MEDICATION_ADMIN_NOT_FOUND")
    })
    @PostMapping("/tagging/{taggingId}/confirm")
    public ResponseEntity<ApiResponse<MedicationAdministrationWriteResponse>> confirm(
            @Parameter(description = "NFC 태깅 묶음 ID", example = "8b2a3f6c-7d4e-4a1b-9c2d-1e2f3a4b5c6d")
            @PathVariable String taggingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationAdministrationWriteResponse data = medicationAdministrationService.confirm(
                taggingId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("투약 기록을 확정했습니다.", data));
    }

    @Operation(summary = "투약 그룹 수정",
            description = "effectiveDatetime은 그룹 일괄, medications.dosageQuantity/dosageUnit은 medicationAdminId 단건씩 수정. 상태 무관.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 외 medicationAdminId — INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 투약 아님 — MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 없음 — MEDICATION_ADMIN_NOT_FOUND")
    })
    @PatchMapping("/tagging/{taggingId}")
    public ResponseEntity<ApiResponse<MedicationAdministrationWriteResponse>> update(
            @Parameter(description = "NFC 태깅 묶음 ID") @PathVariable String taggingId,
            @Valid @RequestBody MedicationAdministrationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationAdministrationWriteResponse data = medicationAdministrationService.update(
                taggingId, request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("투약 기록을 수정했습니다.", data));
    }

    @Operation(summary = "투약 그룹 삭제", description = "같은 taggingId의 모든 row를 통째로 hard delete.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 투약 아님 — MEDICATION_ADMIN_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 없음 — MEDICATION_ADMIN_NOT_FOUND")
    })
    @DeleteMapping("/tagging/{taggingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "NFC 태깅 묶음 ID") @PathVariable String taggingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        medicationAdministrationService.delete(taggingId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("투약 기록을 삭제했습니다.", null));
    }
}
