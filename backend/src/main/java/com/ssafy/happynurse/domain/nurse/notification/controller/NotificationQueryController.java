package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.dto.NotificationListResponse;
import com.ssafy.happynurse.domain.nurse.notification.service.NotificationQueryService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "알림함", description = "병동/개인 알림함 조회 REST API (cursor 기반 페이지네이션)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final NotificationQueryService service;

    @Operation(
            summary = "병동 알림함 조회",
            description = "wardId 의 모든 알림(담당자 무관)을 cursor 페이지네이션으로 반환. " +
                    "JWT 클레임의 wardId 와 요청 wardId 가 일치해야 한다. " +
                    "응답의 recipientPractitionerId 로 클라이언트가 \"내 알림\" 필터링 가능."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT 없음/유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "wardId 권한 불일치 (ROLE_NOT_FOUND)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getWardInbox(
            @RequestParam Long wardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long jwtWardId = principal.getWardId();
        if (jwtWardId == null || !jwtWardId.equals(wardId)) {
            throw new CustomException(ErrorCode.ROLE_NOT_FOUND, "wardId 권한 불일치");
        }
        NotificationListResponse data = service.findWardInbox(wardId, since, before, limit);
        return ResponseEntity.ok(ApiResponse.ok("병동 알림함 조회 성공", data));
    }

    @Operation(
            summary = "개인 알림함 조회",
            description = "JWT 의 practitionerId 를 자동 적용해 본인이 수신한 알림만 cursor 페이지네이션으로 반환. " +
                    "ward 권한 검증 없음 — ward 권한 없는 직무도 사용 가능."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT 없음/유효하지 않음")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<NotificationListResponse>> getPersonalInbox(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails principal) {
        NotificationListResponse data = service.findPersonalInbox(principal.getPractitionerId(), since, before, limit);
        return ResponseEntity.ok(ApiResponse.ok("개인 알림함 조회 성공", data));
    }
}