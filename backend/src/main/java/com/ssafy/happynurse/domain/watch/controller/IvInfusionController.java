package com.ssafy.happynurse.domain.watch.controller;

import com.ssafy.happynurse.domain.watch.dto.ChangeRateRequest;
import com.ssafy.happynurse.domain.watch.dto.IvInfusionListItemResponse;
import com.ssafy.happynurse.domain.watch.dto.IvInfusionResponse;
import com.ssafy.happynurse.domain.watch.dto.StartIvRequest;
import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.service.IvInfusionService;
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

import java.util.List;

@Tag(name = "수액 관리", description = "IV 수액 — 모든 단건 동작은 NFC 태그(tagUid) 기반")
@RestController
@RequestMapping("/iv")
@RequiredArgsConstructor
public class IvInfusionController {

    private final IvInfusionService service;

    @Operation(summary = "수액 시작",
            description = "의사 오더 ID 목록 + 총 용량 + 속도 입력 -> 종료 예상 시점 계산 후 타이머 설정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "NFC 태그 type 불일치 / payload 오류 / 입력 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NFC/처방/약물/의료진 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 처방으로 진행 중 수액 이미 존재")
    })
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<IvInfusionResponse>> start(
            @Valid @RequestBody StartIvRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        IvInfusionResponse data = service.start(request, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("수액을 시작했습니다.", data));
    }

    @Operation(summary = "수액 상세 — NFC 재태깅으로 조회", description = "화면 진입/복귀 시 NFC 재스캔으로 진행 중 IV 복원")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NFC 또는 IV 없음")
    })
    @GetMapping("/by-tag/{tagUid}")
    public ResponseEntity<ApiResponse<IvInfusionResponse>> getByTag(
            @Parameter(description = "NFC tagUid") @PathVariable String tagUid
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDetailByTag(tagUid)));
    }

    @Operation(summary = "주입 속도 변경 — NFC 태깅 기반",
            description = "잔여 용량 새 속도로 재계산해 expectedEndAt 갱신, 5분 전 알림 플래그 NULL 리셋")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IN_PROGRESS 가 아니거나 입력 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NFC 또는 IV 없음")
    })
    @PatchMapping("/by-tag/{tagUid}/rate")
    public ResponseEntity<ApiResponse<IvInfusionResponse>> changeRateByTag(
            @Parameter(description = "NFC tagUid") @PathVariable String tagUid,
            @Valid @RequestBody ChangeRateRequest request
    ) {
        IvInfusionResponse data = service.changeRateByTag(tagUid, request);
        return ResponseEntity.ok(ApiResponse.ok("주입 속도를 변경했습니다.", data));
    }

    @Operation(summary = "수동 종료 — NFC 태깅 기반", description = "status=COMPLETED, actualEndAt=now, 인메모리 잡 cancel")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IN_PROGRESS 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NFC 또는 IV 없음")
    })
    @PostMapping("/by-tag/{tagUid}/complete")
    public ResponseEntity<ApiResponse<IvInfusionResponse>> completeByTag(
            @Parameter(description = "NFC tagUid") @PathVariable String tagUid
    ) {
        IvInfusionResponse data = service.completeByTag(tagUid);
        return ResponseEntity.ok(ApiResponse.ok("수액을 종료했습니다.", data));
    }

    @Operation(summary = "병동 수액 목록", description = "wardId 의 모든 수액 (status 미지정) 또는 특정 status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<IvInfusionListItemResponse>>> listByWard(
            @Parameter(description = "병동 PK") @RequestParam Long wardId,
            @Parameter(description = "수액 상태 필터 (선택)", example = "IN_PROGRESS")
            @RequestParam(required = false) InfusionStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.listByWard(wardId, status)));
    }
}