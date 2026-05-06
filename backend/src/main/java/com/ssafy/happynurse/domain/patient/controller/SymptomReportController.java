package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.SymptomReportListResponse;
import com.ssafy.happynurse.domain.patient.service.SymptomReportService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "환자", description = "환자 API")
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class SymptomReportController {

    private final SymptomReportService symptomReportService;

    @Operation(summary = "환자별 요청 조회",
            description = "해당 환자의 in_progress 입원 동안 제출된 모든 요청을 submittedAt DESC로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 병동의 환자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 또는 입원 정보 없음")
    })
    @GetMapping("/{patientId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomReportListResponse>> getSymptoms(
            @Parameter(description = "환자 ID", example = "1") @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SymptomReportListResponse data = symptomReportService.getSymptomsByPatientId(
                patientId, userDetails.getWardId());
        return ResponseEntity.ok(ApiResponse.ok("환자 증상 보고 목록 조회에 성공했습니다.", data));
    }
}