package com.ssafy.happynurse.domain.webapp.dto;

import jakarta.validation.constraints.NotNull;

public record DevVerifyRequest(
        @NotNull(message = "환자 ID는 필수입니다.")
        Long patientId
) {}