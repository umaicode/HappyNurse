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

@Tag(name = "SSE", description = "간호사 실시간 알림 구독 API")
@RestController
@RequiredArgsConstructor
public class SsePersonalController {

    private final PersonalEmitterRegistry personalEmitterRegistry;

    @Operation(
            summary = "간호사 SSE 구독 (개인 채널)",
            description = "간호사 JWT 기반 개인 채널 구독. 본인 담당 환자 이벤트가 발송된다. " +
                    "Step 2 이후 데스크 PC 용 ward 채널은 별도 엔드포인트(/sse/ward-subscribe)로 분리될 예정."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "간호사 JWT 없음")
    })
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return personalEmitterRegistry.register(userDetails.getPractitionerId());
    }
}