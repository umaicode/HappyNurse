package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.PatientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "수액 주입 속도 변경 요청 — gtt/min + patientType")
public record ChangeRateRequest(

        @Schema(description = "주입 속도 (gtt/min)", example = "40")
        @Positive Integer rateGttPerMin,

        @Schema(description = "환자 타입 — gtt/min 입력 시 필수", example = "ADULT")
        PatientType patientType
) {
    public BigDecimal resolveRateMlPerHr() {
        return RateInputResolver.resolve(rateGttPerMin, patientType);
    }
}
