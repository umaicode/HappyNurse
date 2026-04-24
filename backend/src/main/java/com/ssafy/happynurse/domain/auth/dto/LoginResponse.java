package com.ssafy.happynurse.domain.auth.dto;

public record LoginResponse(
        Long practitionerId,
        String name,
        String employeeNumber,
        String roleCode,
        Long organizationId,
        Long wardId
) {
}
