package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.dto.NursingRecordUpdateRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordWriteResponse;
import com.ssafy.happynurse.domain.nurse.service.NursingRecordService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "간호 기록", description = "간호 기록 확정·수정·삭제 API")
@RestController
@RequestMapping("/nursing-records")
@RequiredArgsConstructor
public class NursingRecordController {

    private final NursingRecordService nursingRecordService;

    @Operation(summary = "간호 기록 확정",
            description = "draft 상태의 간호 기록을 confirmed로 전환. finalContent=editContent 복사, confirmedAt=createdAt 복사.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "draft 아님 — INVALID_RECORD_STATUS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성 아님 — NURSING_RECORD_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "간호 기록 없음 — NURSING_RECORD_NOT_FOUND")
    })
    @PostMapping("/{nursingRecordId}/confirm")
    public ResponseEntity<ApiResponse<NursingRecordWriteResponse>> confirm(
            @Parameter(description = "간호 기록 PK", example = "12") @PathVariable Long nursingRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NursingRecordWriteResponse data = nursingRecordService.confirm(nursingRecordId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 확정했습니다.", data));
    }

    @Operation(summary = "간호 기록 수정",
            description = "본문(content)·확정 시각(confirmedAt)을 부분 수정. content 포함 시 status별로 editContent 또는 finalContent를 갱신(후자는 status=amended).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본문이 비어있음 — INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성 아님 — NURSING_RECORD_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "간호 기록 없음 — NURSING_RECORD_NOT_FOUND")
    })
    @PatchMapping("/{nursingRecordId}")
    public ResponseEntity<ApiResponse<NursingRecordWriteResponse>> update(
            @Parameter(description = "간호 기록 PK", example = "12") @PathVariable Long nursingRecordId,
            @RequestBody NursingRecordUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NursingRecordWriteResponse data = nursingRecordService.update(nursingRecordId, request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 수정했습니다.", data));
    }

    @Operation(summary = "간호 기록 삭제", description = "본인이 작성한 간호 기록을 삭제(hard delete).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 작성 아님 — NURSING_RECORD_NOT_AUTHOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "간호 기록 없음 — NURSING_RECORD_NOT_FOUND")
    })
    @DeleteMapping("/{nursingRecordId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "간호 기록 PK", example = "12") @PathVariable Long nursingRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        nursingRecordService.delete(nursingRecordId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("간호 기록을 삭제했습니다.", null));
    }
}
