package com.ssafy.happynurse.domain.his.dto;

import java.math.BigDecimal;

public record HisOrderItemResponse(
        Long medicationOrderId,
        String orderType,
        String orderCode,
        String orderName,
        BigDecimal dose,
        Integer frequency,
        String doseUnit,
        String route,
        String remarks
) {}
