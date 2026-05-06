package com.ssafy.happynurse.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 마이페이지 프로필 응답")
public record AppProfileResponse(
        @Schema(description = "Practitioner PK", example = "1")
        Long practitionerId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "사원번호", example = "EMP001")
        String employeeNumber,
        @Schema(description = "역할 코드", example = "nurse")
        String roleCode,
        @Schema(description = "병동 ID", example = "3")
        Long wardId,
        @Schema(description = "병동명", example = "내과 3병동")
        String wardName,
        @Schema(description = "기관 ID", example = "1")
        Long organizationId,
        @Schema(description = "기관명", example = "삼성서울병원")
        String organizationName
) {}
