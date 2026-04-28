package com.ssafy.happynurse.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppRefreshRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.") String refreshToken
) {
}
