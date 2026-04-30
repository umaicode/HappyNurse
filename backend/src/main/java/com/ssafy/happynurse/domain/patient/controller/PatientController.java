package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.PatientResonse;
import com.ssafy.happynurse.domain.patient.service.PatientService;
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
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "환자 정보 조회", description = "환자 ID로 환자 기본 정보와 현재 입원 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 또는 입원 정보 없음")
    })
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientResonse>> getPatient(
            @Parameter(description = "환자 ID") @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.getPatient(patientId, userDetails.getPractitionerId())));
    }
}
