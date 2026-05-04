package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.service.NursingNoteService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "간호 기록", description = "간호 노트(STT 기록 + NFC 투약) 조회 API")
@RestController
@RequestMapping("/encounters")
@RequiredArgsConstructor
public class NursingNoteController {

    private final NursingNoteService nursingNoteService;

    @Operation(summary = "입원별 간호 기록 조회",
            description = "한 입원의 STT 간호 기록과 NFC 태깅 투약 기록을 통합해 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 병동 외 입원 — ENCOUNTER_NOT_IN_MY_WARD"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "입원 없음 — ENCOUNTER_NOT_FOUND")
    })
    @GetMapping("/{encounterId}/nursing-notes")
    public ResponseEntity<ApiResponse<List<NursingNoteItemResponse>>> getNursingNotes(
            @Parameter(description = "입원 PK", example = "42") @PathVariable Long encounterId,
            @Parameter(description = "조회 날짜", example = "2026-05-03")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<NursingNoteItemResponse> data = nursingNoteService.getNursingNotes(
                encounterId, date, userDetails.getPractitionerId(), userDetails.getWardId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록 통합 조회에 성공했습니다.", data));
    }
}
