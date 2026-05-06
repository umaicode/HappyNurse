package com.ssafy.happynurse.domain.his.controller;

import com.ssafy.happynurse.domain.his.dto.HisEncounterResponse;
import com.ssafy.happynurse.domain.his.dto.HisNurseResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderCreateRequest;
import com.ssafy.happynurse.domain.his.dto.HisOrderItemResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderUpdateRequest;
import com.ssafy.happynurse.domain.his.service.HisOrderSimulatorService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/his")
@RequiredArgsConstructor
public class HisOrderController {

    private final HisOrderSimulatorService hisOrderSimulatorService;

    @Value("${simulator.api-key}")
    private String simulatorApiKey;

    @PostMapping("/medication-order")
    public ResponseEntity<ApiResponse<HisOrderResponse>> createOrder(
            @RequestHeader(value = "X-Simulator-Key", required = false) String apiKey,
            @Valid @RequestBody HisOrderCreateRequest request) {

        validateApiKey(apiKey);
        Long orderId = hisOrderSimulatorService.create(request);
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 발사 성공", new HisOrderResponse(orderId)));
    }

    @PatchMapping("/medication-order/{id}")
    public ResponseEntity<ApiResponse<HisOrderResponse>> updateOrder(
            @RequestHeader(value = "X-Simulator-Key", required = false) String apiKey,
            @PathVariable Long id,
            @RequestBody HisOrderUpdateRequest request) {

        validateApiKey(apiKey);
        Long orderId = hisOrderSimulatorService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 변경 성공", new HisOrderResponse(orderId)));
    }

    @GetMapping("/nurses")
    public ResponseEntity<ApiResponse<List<HisNurseResponse>>> getNurses(
            @RequestHeader(value = "X-Simulator-Key", required = false) String apiKey) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(
                ApiResponse.ok("간호사 목록 조회 성공", hisOrderSimulatorService.getNurses()));
    }

    @GetMapping("/nurses/{nurseId}/encounters")
    public ResponseEntity<ApiResponse<List<HisEncounterResponse>>> getEncounters(
            @RequestHeader(value = "X-Simulator-Key", required = false) String apiKey,
            @PathVariable Long nurseId) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(
                ApiResponse.ok("담당 환자 목록 조회 성공", hisOrderSimulatorService.getEncountersByNurse(nurseId)));
    }

    @GetMapping("/encounters/{encounterId}/orders")
    public ResponseEntity<ApiResponse<List<HisOrderItemResponse>>> getOrders(
            @RequestHeader(value = "X-Simulator-Key", required = false) String apiKey,
            @PathVariable Long encounterId) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(
                ApiResponse.ok("의사 오더 목록 조회 성공", hisOrderSimulatorService.getOrdersByEncounter(encounterId)));
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(simulatorApiKey)) {
            throw new CustomException(ErrorCode.INVALID_SIMULATOR_KEY);
        }
    }
}