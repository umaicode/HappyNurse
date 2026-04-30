package com.ssafy.happynurse.domain.doctor.controller;

import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.service.MedicationOrderService;
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

@Tag(name = "환자", description = "환자 API")
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class MedicationOrderController {

    private final MedicationOrderService medicationOrderService;

    @Operation(summary = "환자별 의사 오더 조회", description = "환자 ID로 해당 환자의 의사 오더 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 없음")
    })
    @GetMapping("/{patientId}/orders")
    public ResponseEntity<ApiResponse<MedicationOrderListResponse>> getOrders(
            @Parameter(description = "환자 ID") @PathVariable Long patientId,
            @Parameter(description = "조회 날짜", example = "2026-04-27")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationOrderListResponse data = medicationOrderService.getOrdersByPatientId(patientId, date);
        return ResponseEntity.ok(ApiResponse.ok("환자 오더 목록 조회에 성공했습니다.", data));
    }
}