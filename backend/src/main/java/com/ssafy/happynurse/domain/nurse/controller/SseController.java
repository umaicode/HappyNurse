package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.service.SseEmitterManager;
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

@Tag(name = "SSE", description = "간호사 실시간 알림 구독 API")
@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager sseEmitterManager;

    @Operation(
            summary = "간호사 SSE 구독",
            description = "간호사가 웹 대시보드에 로그인 후 이 엔드포인트에 연결하면 " +
                    "담당 환자가 증상을 제출할 때마다 실시간으로 알림을 수신합니다. " +
                    "간호사 JWT가 필요합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "간호사 JWT 없음")
    })
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sseEmitterManager.register(userDetails.getPractitionerId());
    }
}
