package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.OrganizationListResponse;
import com.ssafy.happynurse.domain.patient.dto.WardListResponse;
import com.ssafy.happynurse.domain.patient.service.OrganizationService;
import com.ssafy.happynurse.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "기관", description = "병원/병동 조회 API")
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "병원 목록 조회", description = "활성(active=true) 상태인 병원 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationListResponse>>> listOrganizations() {
        List<OrganizationListResponse> data = organizationService.listActiveOrganizations();
        return ResponseEntity.ok(ApiResponse.ok("병원 목록 조회에 성공했습니다.", data));
    }

    @Operation(summary = "병동 목록 조회", description = "특정 병원의 병동 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기관 없음 — ORGANIZATION_NOT_FOUND")
    })
    @GetMapping("/{organizationId}/wards")
    public ResponseEntity<ApiResponse<List<WardListResponse>>> listWards(
            @PathVariable Long organizationId) {

        List<WardListResponse> data = organizationService.listWardsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("병동 목록 조회에 성공했습니다.", data));
    }
}
