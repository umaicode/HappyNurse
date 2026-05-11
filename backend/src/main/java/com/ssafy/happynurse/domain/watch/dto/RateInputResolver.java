package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주입 속도 입력 정규화 — DB 는 mL/hr 단일 단위, 간호사 입력은 gtt/min + 세트(dropSet)
 */
public final class RateInputResolver {

    private RateInputResolver() {}

    public static BigDecimal resolve(Integer rateGttPerMin, DropSet dropSet) {
        boolean hasGtt = rateGttPerMin != null;

        if (hasGtt && dropSet != null) {
            return BigDecimal.valueOf(rateGttPerMin.longValue() * 60L)
                    .divide(BigDecimal.valueOf(dropSet.getDropFactor()), 2, RoundingMode.HALF_UP);
        }
        throw new CustomException(ErrorCode.IV_RATE_INPUT_INVALID);
    }

    /** mL/hr → gtt/min 역환산 */
    public static Integer toGttPerMin(BigDecimal mlPerHr, DropSet dropSet) {
        if (mlPerHr == null || dropSet == null) {
            throw new CustomException(ErrorCode.IV_RATE_INPUT_INVALID);
        }
        return mlPerHr.multiply(BigDecimal.valueOf(dropSet.getDropFactor()))
                .divide(BigDecimal.valueOf(60L), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
