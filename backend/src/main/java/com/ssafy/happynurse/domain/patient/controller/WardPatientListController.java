package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.WardPatientListResponse;
import com.ssafy.happynurse.domain.patient.service.WardPatientListService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "환자", description = "환자 명단 조회 API")
@RestController
@RequestMapping("/wards/me/patients")
@RequiredArgsConstructor
public class WardPatientListController {

    private final WardPatientListService wardPatientListService;

    @Operation(summary = "병동 입원 환자 목록 조회",
            description = "토큰의 wardId 기준 현재 입원 중(in_progress) 환자를 반환합니다. " +
                    "각 환자에는 본인 담당 여부(isMyPatient)가 Redis 저장 기준으로 함께 표시됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<WardPatientListResponse>>> listMyWardPatients(
            @AuthenticationPrincipal CustomUserDetails user) {

        List<WardPatientListResponse> data = wardPatientListService.listWardPatients(
                user.getWardId(), user.getPractitionerId());

        return ResponseEntity.ok(ApiResponse.ok("병동 환자 목록 조회에 성공했습니다.", data));
    }
}
