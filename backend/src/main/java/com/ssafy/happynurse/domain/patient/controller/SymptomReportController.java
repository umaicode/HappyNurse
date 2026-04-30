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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "환자 요청", description = "환자 증상 보고 조회 API")
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class SymptomReportController {

    private final SymptomReportService symptomReportService;

    @Operation(summary = "환자별 증상 보고 조회", description = "특정 날짜에 해당 환자가 제출한 증상 보고 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 병동의 환자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 또는 입원 정보 없음")
    })
    @GetMapping("/{patientId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomReportListResponse>> getSymptoms(
            @Parameter(description = "환자 ID") @PathVariable Long patientId,
            @Parameter(description = "조회 날짜", example = "2026-04-29")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SymptomReportListResponse data = symptomReportService.getSymptomsByPatientId(
                patientId, userDetails.getWardId(), date);
        return ResponseEntity.ok(ApiResponse.ok("환자 증상 보고 목록 조회에 성공했습니다.", data));
    }
}