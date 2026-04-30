package com.ssafy.happynurse.domain.doctor.dto;

import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.entity.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MedicationOrderItemResponse(
        @Schema(description = "오더 PK", example = "9")
        Long medicationOrderId,

        @Schema(description = "오더 구분", example = "FLUID")
        OrderType orderType,

        @Schema(description = "처방 코드", example = "IV5001")
        String orderCode,

        @Schema(description = "처방 명칭", example = "5% Dextrose Inj. 1L")
        String orderName,

        @Schema(description = "1회 용량", example = "1000")
        BigDecimal dose,

        @Schema(description = "투여 횟수", example = "1")
        Integer frequency,

        @Schema(description = "용량 단위", example = "bag")
        String doseUnit,

        @Schema(description = "투여 경로", example = "IV")
        String route,

        @Schema(description = "참고사항", example = "60cc/hr 유지.")
        String remarks,

        @Schema(description = "오더 상태", example = "active")
        OrderStatus status,

        @Schema(description = "처방 작성 시간", example = "2026-04-27T14:00:00")
        LocalDateTime dateWritten,

        @Schema(description = "처방 의사 PK", example = "6")
        Long prescriberId,

        @Schema(description = "처방 의사 이름", example = "이조은")
        String prescriberName
) {
}