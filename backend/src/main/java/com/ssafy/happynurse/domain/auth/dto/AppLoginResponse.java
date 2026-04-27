package com.ssafy.happynurse.domain.auth.dto;

public record AppLoginResponse(
        String accessToken,
        String refreshToken,
        Long practitionerId,
        String name,
        String employeeNumber,
        String roleCode,
        Long organizationId,
        Long wardId
) {
    public static AppLoginResponse from(AuthResult result) {
        LoginResponse lr = result.loginResponse();
        return new AppLoginResponse(
                result.accessToken(),
                result.refreshToken(),
                lr.practitionerId(),
                lr.name(),
                lr.employeeNumber(),
                lr.roleCode(),
                lr.organizationId(),
                lr.wardId()
        );
    }
}
