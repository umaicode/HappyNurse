package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.PersonalEmitterRegistry;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "SSE", description = "간호사 ward/개인 채널 실시간 알림 구독 API")
@RestController
@RequiredArgsConstructor
public class SsePersonalController {

    private final PersonalEmitterRegistry personalEmitterRegistry;

    @Operation(
            summary = "간호사 SSE 구독 (개인 채널)",
            description = "간호사 JWT 기반 개인 채널 구독. 본인(assignedPractitionerId) 담당 알림이 발송된다. " +
                    "ward 전체 알림 구독은 별도 엔드포인트 /sse/ward-subscribe 사용."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "간호사 JWT 없음")
    })
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                HttpServletResponse response) throws IOException {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        SseEmitter emitter = personalEmitterRegistry.register(userDetails.getPractitionerId());
        emitter.send(SseEmitter.event().name("ready").data("ok"));

        return emitter;
    }
}