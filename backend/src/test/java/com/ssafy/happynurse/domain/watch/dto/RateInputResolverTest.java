package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.PatientType;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateInputResolverTest {

    @Test
    @DisplayName("gtt/min + ADULT(20) → (gtt × 60) / 20 = mL/hr")
    void resolve_성인_환산() {
        // 60 gtt/min × 60 / 20 = 180 mL/hr
        BigDecimal result = RateInputResolver.resolve(60, PatientType.ADULT);
        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("gtt/min + PEDIATRIC(60) → (gtt × 60) / 60 = mL/hr")
    void resolve_소아_환산() {
        // 60 gtt/min × 60 / 60 = 60 mL/hr
        BigDecimal result = RateInputResolver.resolve(60, PatientType.PEDIATRIC);
        assertThat(result).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("gtt/min null → IV_RATE_INPUT_INVALID")
    void resolve_gtt_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.resolve(null, PatientType.ADULT))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }

    @Test
    @DisplayName("patientType null → IV_RATE_INPUT_INVALID")
    void resolve_patientType_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.resolve(60, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }
}
