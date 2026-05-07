package com.ssafy.happynurse.domain.his.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HisOrderCreateRequest(
        @NotNull Long patientId,
        @NotNull Long encounterId,
        @NotNull Long prescriberId,
        Long medicationId,
        @NotBlank String orderType,
        @NotBlank String orderCode,
        @NotBlank String orderName,
        BigDecimal dose,
        Integer frequency,
        String doseUnit,
        String route,
        String remarks
) {}