package com.ssafy.happynurse.domain.his.controller;

import com.ssafy.happynurse.domain.his.dto.HisEncounterCreateRequest;
import com.ssafy.happynurse.domain.his.dto.HisEncounterResultResponse;
import com.ssafy.happynurse.domain.his.service.HisEncounterSimulatorService;
import com.ssafy.happynurse.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "HIS 시뮬레이터", description = "HIS 시뮬레이터 — 의사 오더 / 입퇴원 이벤트 발사")
@RestController
@RequestMapping("/his")
@RequiredArgsConstructor
public class HisEncounterController {

    private final HisEncounterSimulatorService service;

    @Operation(summary = "입원 발사", description = "Encounter 를 in_progress 로 신규 INSERT (periodStart = now)")
    @PostMapping("/encounters")
    public ResponseEntity<ApiResponse<HisEncounterResultResponse>> admit(
            @Valid @RequestBody HisEncounterCreateRequest request) {

        Long encounterId = service.admit(request);
        return ResponseEntity.ok(
                ApiResponse.ok("입원 발사 성공", new HisEncounterResultResponse(encounterId)));
    }

    @Operation(summary = "퇴원 발사",
            description = "Encounter 상태를 finished 로 마감 (periodEnd = now). 이미 퇴원된 경우 409.")
    @PatchMapping("/encounters/{encounterId}/discharge")
    public ResponseEntity<ApiResponse<HisEncounterResultResponse>> discharge(
            @PathVariable Long encounterId) {

        Long id = service.discharge(encounterId);
        return ResponseEntity.ok(
                ApiResponse.ok("퇴원 발사 성공", new HisEncounterResultResponse(id)));
    }
}
