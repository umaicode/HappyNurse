package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.AppProfileResponse;
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
@RequestMapping("/app/practitioners")
@RequiredArgsConstructor
public class AppPractitionerController {

    private final PractitionerService practitionerService;

    @Operation(summary = "앱 마이페이지 프로필 조회",
            description = "로그인한 간호사의 확장된 프로필(이름, 사원번호, 역할, 병동, 기관)을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Practitioner/역할/기관 없음")
    })
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<AppProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AppProfileResponse response = practitionerService.getAppProfile(
                userDetails.getPractitionerId(),
                userDetails.getWardId(),
                userDetails.getOrganizationId());

        return ResponseEntity.ok(ApiResponse.ok("프로필 조회에 성공했습니다.", response));
    }
}
