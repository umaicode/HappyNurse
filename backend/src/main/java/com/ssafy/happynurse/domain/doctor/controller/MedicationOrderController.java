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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "의사 오더", description = "의사 오더 조회 API")
@RestController
@RequestMapping("/encounters")
@RequiredArgsConstructor
public class MedicationOrderController {

    private final MedicationOrderService medicationOrderService;

    @Operation(summary = "입원별 의사 오더 조회",
            description = "입원 ID로 해당 입원에 속한 의사 오더 전체를 dateWritten 내림차순으로 조회합니다. 응답은 createdAt·updatedAt도 포함하여 status 변경 시점을 노출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "입원 없음 — ENCOUNTER_NOT_FOUND")
    })
    @GetMapping("/{encounterId}/orders")
    public ResponseEntity<ApiResponse<MedicationOrderListResponse>> getOrders(
            @Parameter(description = "입원 ID", example = "42") @PathVariable Long encounterId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MedicationOrderListResponse data = medicationOrderService.getOrdersByEncounterId(encounterId);
        return ResponseEntity.ok(ApiResponse.ok("입원별 의사 오더 조회에 성공했습니다.", data));
    }
}
