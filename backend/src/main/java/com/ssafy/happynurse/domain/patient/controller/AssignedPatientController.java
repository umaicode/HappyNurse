package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateRequest;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateResponse;
import com.ssafy.happynurse.domain.patient.service.AssignedPatientService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "담당 환자", description = "본인 담당 환자 관리 API")
@RestController
@RequestMapping("/practitioners/me/patients")
@RequiredArgsConstructor
public class AssignedPatientController {

    private final AssignedPatientService assignedPatientService;

    @Operation(summary = "내 담당 환자 일괄 저장",
            description = "체크된 입원(encounterId) 목록을 받아 본인을 담당으로 지정하고, 그 외 본인 담당이던 입원은 해제합니다. " +
                    "Redis 에 본인 선택을 저장하여 시프트 교대 후에도 복원됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 병동 외 입원 ID 포함")
    })
    @PutMapping
    public ResponseEntity<ApiResponse<AssignedPatientUpdateResponse>> updateMyAssignedPatients(
            @Valid @RequestBody AssignedPatientUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        AssignedPatientUpdateResponse data = assignedPatientService.updateMyAssignedPatients(
                user.getPractitionerId(), user.getWardId(), request);

        return ResponseEntity.ok(ApiResponse.ok("담당 환자 저장에 성공했습니다.", data));
    }
}
