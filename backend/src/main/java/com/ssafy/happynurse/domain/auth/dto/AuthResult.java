package com.ssafy.happynurse.domain.auth.dto;

public record AuthResult(
        String accessToken,
        LoginResponse loginResponse
) {
}
