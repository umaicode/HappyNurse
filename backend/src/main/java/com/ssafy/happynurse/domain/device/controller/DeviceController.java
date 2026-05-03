package com.ssafy.happynurse.domain.device.controller;

import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterRequest;
import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterResponse;
import com.ssafy.happynurse.domain.device.service.DeviceService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "디바이스", description = "FCM 디바이스 토큰 등록 API")
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "FCM 디바이스 토큰 등록",
            description = "현재 로그인 한 간호사의 활성 ward 에 디바이스 토큰을 UPSERT 등록한다")
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<FcmTokenRegisterResponse>> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRegisterRequest request) {

        FcmTokenRegisterResponse response = deviceService.registerFcmToken(
                userDetails.getPractitionerId(),
                userDetails.getWardId(),
                request);

        return ResponseEntity.ok(ApiResponse.ok("FCM 토큰 등록 성공", response));
    }
}