package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "수액 주입 속도 변경 요청 — gtt/min + dropSet")
public record ChangeRateRequest(

        @Schema(description = "주입 속도 (gtt/min)", example = "40")
        @Positive Integer rateGttPerMin,

        @Schema(description = "수액 세트 (gtt/mL) — gtt/min 입력 시 필수", example = "SET_20")
        DropSet dropSet
) {
    public BigDecimal resolveRateMlPerHr() {
        return RateInputResolver.resolve(rateGttPerMin, dropSet);
    }
}
