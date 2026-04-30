package com.ssafy.happynurse.domain.webapp.controller;

import com.ssafy.happynurse.domain.webapp.dto.FaqListResponse;
import com.ssafy.happynurse.domain.webapp.service.FaqService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patient Webapp", description = "환자 웹앱 API")
@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @Operation(
            summary = "환자 FAQ 조회",
            description = "환자의 병명·수술명·주증상에 따라 정렬된 FAQ 리스트를 반환합니다. " +
                    "병명이 매칭되지 않으면 items는 빈 배열이고 matchedFaqDisease는 null입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "환자 JWT 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 아닌 환자 ID 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자 또는 활성 입원 없음")
    })
    @GetMapping("/patients/{patientId}/faq")
    public ResponseEntity<ApiResponse<FaqListResponse>> getFaq(
            @Parameter(description = "환자 ID (JWT subject와 일치해야 함)", example = "1", required = true)
            @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                faqService.getFaq(userDetails.getPractitionerId(), patientId)
        ));
    }
}