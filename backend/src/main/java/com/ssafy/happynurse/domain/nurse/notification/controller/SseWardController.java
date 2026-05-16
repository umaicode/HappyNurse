package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 데스크 PC 가 ward 단위 알림을 구독하기 위한 SSE 엔드포인트.
 *
 * 인증: 기존 JwtAuthenticationFilter — Authorization: Bearer <JWT>
 * wardId 출처: JWT 의 wardId 클레임 (JwtTokenProvider.createAccessToken() 발급 시 포함)
 */
@Tag(name = "SSE", description = "간호사 ward 채널 실시간 알림 구독 API")
@RestController
@RequiredArgsConstructor
public class SseWardController {

    private final WardEmitterRegistry wardEmitterRegistry;

    @Operation(
            summary = "간호사 ward SSE 구독 (병동 데스크 PC 채널)",
            description = "간호사 JWT 의 wardId 클레임 기준으로 ward 채널을 구독한다. " +
                    "같은 ward 의 모든 알림 (담당자 무관) 을 데스크 PC 화면에 표시하기 위함. " +
                    "예: 와이어프레임의 \"수액타이머\" 탭 카운트다운 등."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공"),
            @ApiResponse(responseCode = "401", description = "JWT 없음/유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "JWT 에 wardId 클레임 없음")
    })
    @GetMapping(value = "/sse/ward-subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                HttpServletResponse response) throws IOException {
        Long wardId = userDetails.getWardId();
        if (wardId == null) {
            throw new CustomException(ErrorCode.ROLE_NOT_FOUND, "JWT 클레임에 wardId 없음 — ward 채널 구독 불가");
        }

        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        SseEmitter emitter = wardEmitterRegistry.register(wardId);
        emitter.send(SseEmitter.event().name("ready").data("ok"));

        return emitter;
    }
}