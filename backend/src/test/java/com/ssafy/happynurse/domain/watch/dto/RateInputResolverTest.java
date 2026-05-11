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

    @Test
    @DisplayName("toGttPerMin - ADULT(20): 180 mL/hr → 60 gtt/min")
    void toGtt_성인_역환산() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("180"), PatientType.ADULT);
        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("toGttPerMin - PEDIATRIC(60): 60 mL/hr → 60 gtt/min")
    void toGtt_소아_역환산() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("60"), PatientType.PEDIATRIC);
        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("toGttPerMin - 반올림: 100 mL/hr × 20 / 60 = 33.33... → 33")
    void toGtt_반올림() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("100"), PatientType.ADULT);
        assertThat(result).isEqualTo(33);
    }

    @Test
    @DisplayName("toGttPerMin - mlPerHr null → IV_RATE_INPUT_INVALID")
    void toGtt_mlPerHr_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.toGttPerMin(null, PatientType.ADULT))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }

    @Test
    @DisplayName("toGttPerMin - patientType null → IV_RATE_INPUT_INVALID")
    void toGtt_patientType_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.toGttPerMin(new BigDecimal("100"), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }
}
