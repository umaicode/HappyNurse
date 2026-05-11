package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.PatientType;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주입 속도 입력 정규화 — DB 는 mL/hr 단일 단위, 간호사 입력은 gtt/mL
 */
public final class RateInputResolver {

    private RateInputResolver() {}

    public static BigDecimal resolve(Integer rateGttPerMin, PatientType patientType) {
        boolean hasGtt = rateGttPerMin != null;

        if (hasGtt && patientType != null) {
            return BigDecimal.valueOf(rateGttPerMin.longValue() * 60L)
                    .divide(BigDecimal.valueOf(patientType.getDropFactor()), 2, RoundingMode.HALF_UP);
        }
        throw new CustomException(ErrorCode.IV_RATE_INPUT_INVALID);
    }

    /** mL/hr → gtt/min 역환산 */
    public static Integer toGttPerMin(BigDecimal mlPerHr, PatientType patientType) {
        if (mlPerHr == null || patientType == null) {
            throw new CustomException(ErrorCode.IV_RATE_INPUT_INVALID);
        }
        return mlPerHr.multiply(BigDecimal.valueOf(patientType.getDropFactor()))
                .divide(BigDecimal.valueOf(60L), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}