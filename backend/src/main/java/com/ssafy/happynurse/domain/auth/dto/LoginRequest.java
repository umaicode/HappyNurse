package com.ssafy.happynurse.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "기관 ID는 필수입니다.") Long organizationId,
        @NotNull(message = "병동 ID는 필수입니다.") Long wardId,
        @NotBlank(message = "사원번호는 필수입니다.") String employeeNumber,
        @NotBlank(message = "비밀번호는 필수입니다.") String password
) {
}
