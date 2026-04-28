package com.ssafy.happynurse.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PractitionerMeResponse(
        @Schema(description = "Practitioner PK", example = "1")
        Long practitionerId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "사원번호", example = "EMP001")
        String employeeNumber,

        @Schema(description = "직급 코드", example = "nurse")
        String roleCode,

        @Schema(description = "소속 병동 PK", example = "3")
        Long wardId,

        @Schema(description = "소속 병동명", example = "내과 3병동")
        String wardName
) {
}
