package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.PractitionerMeResponse;
import com.ssafy.happynurse.domain.auth.service.PractitionerService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "Practitioner(의료진) 정보 API")
@RestController
@RequestMapping("/practitioners")
@RequiredArgsConstructor
public class PractitionerController {

    private final PractitionerService practitionerService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 기본 정보와 현재 병동 역할을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Practitioner 또는 활성 역할 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PractitionerMeResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PractitionerMeResponse response = practitionerService.getMyInfo(
                userDetails.getPractitionerId(),
                userDetails.getWardId());

        return ResponseEntity.ok(ApiResponse.ok("내 정보 조회에 성공했습니다.", response));
    }
}