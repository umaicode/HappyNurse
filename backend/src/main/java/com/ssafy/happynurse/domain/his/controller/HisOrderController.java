package com.ssafy.happynurse.domain.his.controller;

import com.ssafy.happynurse.domain.his.dto.HisEncounterResponse;
import com.ssafy.happynurse.domain.his.dto.HisNurseResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderCreateRequest;
import com.ssafy.happynurse.domain.his.dto.HisOrderItemResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderUpdateRequest;
import com.ssafy.happynurse.domain.his.service.HisOrderSimulatorService;
import com.ssafy.happynurse.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/his")
@RequiredArgsConstructor
public class HisOrderController {

    private final HisOrderSimulatorService hisOrderSimulatorService;

    @PostMapping("/medication-order")
    public ResponseEntity<ApiResponse<HisOrderResponse>> createOrder(
            @Valid @RequestBody HisOrderCreateRequest request) {

        Long orderId = hisOrderSimulatorService.create(request);
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 발사 성공", new HisOrderResponse(orderId)));
    }

    @PatchMapping("/medication-order/{id}")
    public ResponseEntity<ApiResponse<HisOrderResponse>> updateOrder(
            @PathVariable Long id,
            @RequestBody HisOrderUpdateRequest request) {

        Long orderId = hisOrderSimulatorService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 변경 성공", new HisOrderResponse(orderId)));
    }

    @GetMapping("/nurses")
    public ResponseEntity<ApiResponse<List<HisNurseResponse>>> getNurses() {
        return ResponseEntity.ok(
                ApiResponse.ok("간호사 목록 조회 성공", hisOrderSimulatorService.getNurses()));
    }

    @GetMapping("/nurses/{nurseId}/encounters")
    public ResponseEntity<ApiResponse<List<HisEncounterResponse>>> getEncounters(
            @PathVariable Long nurseId) {
        return ResponseEntity.ok(
                ApiResponse.ok("담당 환자 목록 조회 성공", hisOrderSimulatorService.getEncountersByNurse(nurseId)));
    }

    @GetMapping("/encounters/{encounterId}/orders")
    public ResponseEntity<ApiResponse<List<HisOrderItemResponse>>> getOrders(
            @PathVariable Long encounterId) {
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 목록 조회 성공", hisOrderSimulatorService.getOrdersByEncounter(encounterId)));
    }
}
