package com.ssafy.happynurse.domain.handover.controller;

import com.ssafy.happynurse.domain.handover.dto.HandoverChecksPatchRequest;
import com.ssafy.happynurse.domain.handover.dto.HandoverDetailResponse;
import com.ssafy.happynurse.domain.handover.service.ShiftHandoverService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인계", description = "인수인계 리포트 단건 체크리스트 토글")
@RestController
@RequestMapping("/handover")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService service;

    @Operation(summary = "인수인계 체크리스트 조회",
            description = """
                    handoverId 로 체크리스트 상태(checkedItemsJson)만 조회
                    PASS-BAR 풀 페이로드는 AI 서버 (`/api/handover/{id}`) 가 담당하고, BE 는 체크 상태만 관리
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트 없음 — HANDOVER_NOT_FOUND")
    })
    @GetMapping("/{handoverId}")
    public ResponseEntity<ApiResponse<HandoverDetailResponse>> getDetail(
            @Parameter(description = "인수인계 PK") @PathVariable Long handoverId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDetail(handoverId)));
    }

    @Operation(summary = "체크리스트 토글",
            description = """
                    체크 상태가 **바뀐 항목만** body 에 담기 (델타 방식, 전체 상태 X)
                    - `"synthesis.0": true`  → 체크 ON  (누른 사람/시각 기록, 이미 있으면 덮어쓰기)
                    - `"synthesis.2": false` → 체크 OFF (해당 키 제거)
                    - body 에 없는 키는 서버에서 **그대로 유지**된다.

                    키 포맷: `synthesis.{index}` (현재 synthesis 슬롯만 허용. 다른 슬롯 키 → 400)
                    저장 형식: `{ "synthesis.0": { "by": <practitionerId>, "at": <iso8601> } }`
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "키 형식 오류 — HANDOVER_CHECK_KEY_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트 없음 — HANDOVER_NOT_FOUND")
    })
    @PatchMapping("/{handoverId}/checks")
    public ResponseEntity<ApiResponse<Void>> patchChecks(
            @Parameter(description = "인수인계 PK") @PathVariable Long handoverId,
            @Valid @RequestBody HandoverChecksPatchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        service.patchChecks(handoverId, request.checks(), userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("체크리스트가 갱신되었습니다.", null));
    }
}
