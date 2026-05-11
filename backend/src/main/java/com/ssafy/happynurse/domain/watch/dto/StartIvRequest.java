package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "수액 시작 요청 — verify 통과한 처방 PK 목록 + IV 설정")
public record StartIvRequest(
        @Schema(description = "Encounter PK (입원 id)", example = "11")
        @NotNull
        Long encounterId,

        @Schema(description = "verify 통과한 처방 PK 목록 (1개 이상) mix IV 면 multi.",
                example = "[1, 2]")
        @NotEmpty
        List<Long> medicationOrderIds,

        @Schema(description = "총 용량 (mL) — 혼합 후 최종 부피", example = "12")
        @NotNull @Positive
        BigDecimal totalVolumeMl,

        @Schema(description = "주입 속도 (gtt/min)", example = "40")
        @NotNull @Positive
        Integer rateGttPerMin,

        @Schema(description = "수액 세트 (gtt/mL): SET_10, SET_15, SET_20, SET_60", example = "SET_20")
        @NotNull
        DropSet dropSet,

        @Schema(description = "메모", example = "6분 알림 테스트")
        String note
) {
    public BigDecimal resolveRateMlPerHr() {
        return RateInputResolver.resolve(rateGttPerMin, dropSet);
    }
}
