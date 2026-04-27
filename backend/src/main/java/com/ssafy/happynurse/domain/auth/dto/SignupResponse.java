package com.ssafy.happynurse.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "회원가입 응답 (생성된 Practitioner + 활성 PractitionerRole 정보)")
public record SignupResponse(
        @Schema(description = "Practitioner ID", example = "42")
        Long practitionerId,

        @Schema(description = "사원번호", example = "DEV001")
        String employeeNumber,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "역할 코드", example = "nurse")
        String roleCode,

        @Schema(description = "기관 ID", example = "1")
        Long organizationId,

        @Schema(description = "병동 ID", example = "1")
        Long wardId,

        @Schema(description = "역할 활성 시작일", example = "2026-04-27")
        LocalDate periodStart
) {
}