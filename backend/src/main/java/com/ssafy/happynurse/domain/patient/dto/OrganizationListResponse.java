package com.ssafy.happynurse.domain.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrganizationListResponse(
        @Schema(description = "기관 PK", example = "1")
        Long organizationId,

        @Schema(description = "병원명", example = "싸피 병원")
        String name,

        @Schema(description = "조직 유형 코드", example = "hospital")
        String typeCode
) {
}
