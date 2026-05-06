package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.service.MedicationAdministrationService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "투약 기록", description = "투약 기록(NFC 태깅 그룹) 삭제 API")
@RestController
@RequestMapping("/medication-administrations")
@RequiredArgsConstructor
public class MedicationAdministrationController {

    private final MedicationAdministrationService medicationAdministrationService;

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
