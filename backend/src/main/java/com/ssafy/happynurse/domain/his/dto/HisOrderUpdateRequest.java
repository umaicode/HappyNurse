package com.ssafy.happynurse.domain.his.dto;

import java.math.BigDecimal;

public record HisOrderUpdateRequest(
        String status,
        BigDecimal dose,
        Integer frequency,
        String doseUnit,
        String route,
        String remarks
) {}