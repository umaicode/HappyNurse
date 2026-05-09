package com.ssafy.happynurse.domain.reminder.controller;

import com.ssafy.happynurse.domain.reminder.dto.CreateSttReminderRequest;
import com.ssafy.happynurse.domain.reminder.dto.PreviewSttReminderRequest;
import com.ssafy.happynurse.domain.reminder.dto.PreviewSttReminderResponse;
import com.ssafy.happynurse.domain.reminder.dto.SttReminderListItemResponse;
import com.ssafy.happynurse.domain.reminder.dto.SttReminderResponse;
import com.ssafy.happynurse.domain.reminder.service.SttReminderService;

import java.util.List;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "STT 음성 메모 알람", description = "워치 STT 발화로 등록되는 미래 시각 알람")
@RestController
@RequestMapping("/reminders/stt")
@RequiredArgsConstructor
public class SttReminderController {

    private final SttReminderService service;

    @Operation(summary = "내 STT 알람 리스트",
            description = "본인이 등록한 미발사(SCHEDULED) STT 음성 메모 알람 — fireAt 오름차순. 워치 홈 STT 탭 카드 리스트용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SttReminderListItemResponse>>> listMine(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.listScheduledOf(userDetails.getPractitionerId())));
    }

    @Operation(summary = "STT 시간 파싱 미리보기",
            description = "STT 발화에서 시간만 파싱해 응답 (저장하지 않음). 워치 검토 화면에서 시간 표시용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "파싱 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "발화에서 시간 표현을 찾지 못함")
    })
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewSttReminderResponse>> preview(
            @Valid @RequestBody PreviewSttReminderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.preview(request.sttText())));
    }

    @Operation(summary = "STT 알람 등록",
            description = "STT 발화 원문에서 시간을 파싱(또는 사용자가 워치에서 수정한 fireAtEpochMillis 사용)해 알람을 등록한다. " +
                    "시각이 되면 워치 풀스크린 알람으로 메모 본문이 표시됨.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "발화에서 시간 표현을 찾지 못함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 또는 의료진 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SttReminderResponse>> create(
            @Valid @RequestBody CreateSttReminderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SttReminderResponse data = service.create(request,
                userDetails.getPractitionerId(),
                userDetails.getWardId());
        return ResponseEntity.ok(ApiResponse.ok("STT 알람을 등록했습니다.", data));
    }

    @Operation(summary = "STT 알람 취소", description = "본인이 등록한 SCHEDULED 상태의 알람만 취소 가능 (멱등).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 알람이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알람 없음")
    })
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long reminderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        service.cancel(reminderId, userDetails.getPractitionerId());
        return ResponseEntity.ok(ApiResponse.ok("STT 알람을 취소했습니다.", null));
    }
}
