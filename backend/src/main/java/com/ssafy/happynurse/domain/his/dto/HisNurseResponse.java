package com.ssafy.happynurse.domain.his.dto;

public record HisNurseResponse(
        Long practitionerId,
        String name,
        String employeeNumber
) {}
