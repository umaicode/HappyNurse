package com.ssafy.happynurse.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "개발용 로그인 요청 (사원번호만으로 로그인)")
public record DevLoginRequest(
        @Schema(description = "사원번호", example = "admin")
        @NotBlank(message = "사원번호는 필수입니다.")
        String employeeNumber
) {
}
