package com.ssafy.happynurse.domain.nurse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "개별 투약 약물 정보")
public record MedicationItemResponse(
        @Schema(description = "투약 기록 PK", example = "31")
        Long medicationAdminId,

        @Schema(description = "제품 코드", example = "642003420")
        String productCode,

        @Schema(description = "제품명", example = "모르핀황산염주사")
        String productName,

        @Schema(description = "1회 투여량", example = "5.000")
        BigDecimal dosageQuantity,

        @Schema(description = "투약 단위", example = "mg")
        String dosageUnit,

        @Schema(description = "투약 횟수 (의사 처방 frequency)", example = "1")
        Integer frequency,

        @Schema(description = "투여 경로 (의사 처방 route)", example = "IV")
        String route,

        @Schema(description = "[IV 한정] 현재 주입 속도 mL/hr. NFC 약물 행은 응답에서 생략됨")
        BigDecimal ivRateMlPerHr
) {
}
