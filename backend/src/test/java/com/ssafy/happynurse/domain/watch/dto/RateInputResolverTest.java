package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateInputResolverTest {

    @Test
    @DisplayName("gtt/min + SET_20 → (gtt × 60) / 20 = mL/hr")
    void resolve_세트20_환산() {
        // 60 gtt/min × 60 / 20 = 180 mL/hr
        BigDecimal result = RateInputResolver.resolve(60, DropSet.SET_20);
        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("gtt/min + SET_60 → (gtt × 60) / 60 = mL/hr")
    void resolve_세트60_환산() {
        // 60 gtt/min × 60 / 60 = 60 mL/hr
        BigDecimal result = RateInputResolver.resolve(60, DropSet.SET_60);
        assertThat(result).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("gtt/min + SET_10 → (gtt × 60) / 10 = mL/hr")
    void resolve_세트10_환산() {
        // 30 gtt/min × 60 / 10 = 180 mL/hr
        BigDecimal result = RateInputResolver.resolve(30, DropSet.SET_10);
        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("gtt/min + SET_15 → (gtt × 60) / 15 = mL/hr")
    void resolve_세트15_환산() {
        // 30 gtt/min × 60 / 15 = 120 mL/hr
        BigDecimal result = RateInputResolver.resolve(30, DropSet.SET_15);
        assertThat(result).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("gtt/min null → IV_RATE_INPUT_INVALID")
    void resolve_gtt_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.resolve(null, DropSet.SET_20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }

    @Test
    @DisplayName("dropSet null → IV_RATE_INPUT_INVALID")
    void resolve_dropSet_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.resolve(60, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }

    @Test
    @DisplayName("toGttPerMin - SET_20: 180 mL/hr → 60 gtt/min")
    void toGtt_세트20_역환산() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("180"), DropSet.SET_20);
        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("toGttPerMin - SET_60: 60 mL/hr → 60 gtt/min")
    void toGtt_세트60_역환산() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("60"), DropSet.SET_60);
        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("toGttPerMin - 반올림: 100 mL/hr × 20 / 60 = 33.33... → 33")
    void toGtt_반올림() {
        Integer result = RateInputResolver.toGttPerMin(new BigDecimal("100"), DropSet.SET_20);
        assertThat(result).isEqualTo(33);
    }

    @Test
    @DisplayName("toGttPerMin - mlPerHr null → IV_RATE_INPUT_INVALID")
    void toGtt_mlPerHr_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.toGttPerMin(null, DropSet.SET_20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }

    @Test
    @DisplayName("toGttPerMin - dropSet null → IV_RATE_INPUT_INVALID")
    void toGtt_dropSet_null_실패() {
        assertThatThrownBy(() -> RateInputResolver.toGttPerMin(new BigDecimal("100"), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_RATE_INPUT_INVALID);
    }
}
