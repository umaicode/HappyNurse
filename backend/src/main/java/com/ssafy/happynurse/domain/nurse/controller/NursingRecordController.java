package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.dto.NursingRecordManualCreateRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordWriteResponse;
import com.ssafy.happynurse.domain.nurse.service.NursingRecordService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "간호 기록", description = "간호 기록 작성 API")
@RestController
@RequestMapping("/nursing-records")
@RequiredArgsConstructor
public class NursingRecordController {

    private final NursingRecordService nursingRecordService;

    @Operation(summary = "간호 기록 수동 작성",
            description = "음성 없이 직접 본문을 입력해 바로 confirmed 상태의 간호 기록을 생성. confirmedAt은 서버 시각으로 설정.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본문이 비어있거나 입력값이 잘못됨 — INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "입원 또는 의료진 없음 — ENCOUNTER_NOT_FOUND / PRACTITIONER_NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<NursingRecordWriteResponse>> createManual(
            @RequestBody NursingRecordManualCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NursingRecordWriteResponse data = nursingRecordService.createManual(request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 작성했습니다.", data));
    }
}
