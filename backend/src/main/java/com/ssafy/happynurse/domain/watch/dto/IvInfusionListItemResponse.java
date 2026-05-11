package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "수액 목록 항목 — 병동 화면용 slim 응답")
public record IvInfusionListItemResponse(
        @Schema(description = "수액 PK") Long ivInfusionId,
        @Schema(description = "환자 PK") Long patientId,
        @Schema(description = "환자 이름") String patientName,
        @Schema(description = "혼합 약물명 목록", example = "[\"5% Dextrose\", \"KCl\"]") List<String> medicationNames,
        @Schema(description = "현재 주입 속도 (mL/hr)") BigDecimal currentRateMlPerHr,
        @Schema(description = "현재 주입 속도 (gtt/min) — dropSet 기반 역환산", example = "40") Integer rateGttPerMin,
        @Schema(description = "수액 세트 (gtt/mL): SET_10, SET_15, SET_20, SET_60", example = "SET_20") DropSet dropSet,
        @Schema(description = "상태") InfusionStatus status,
        @Schema(description = "수액 투여 시작 시각") LocalDateTime startedAt,
        @Schema(description = "예상 종료 시각") LocalDateTime expectedEndAt,
        @Schema(description = "잔여 시간 (초)") Long remainingSeconds
) {
    public static IvInfusionListItemResponse from(IvInfusion iv, LocalDateTime now) {
        long secsLeft = Math.max(0L, Duration.between(now, iv.getExpectedEndAt()).getSeconds());
        List<String> names = iv.getMedications().stream()
                .map(ivm -> ivm.getMedication().getProductName())
                .toList();
        return new IvInfusionListItemResponse(
                iv.getIvInfusionId(),
                iv.getPatient().getPatientId(),
                iv.getPatient().getName(),
                names,
                iv.getCurrentRateMlPerHr(),
                RateInputResolver.toGttPerMin(iv.getCurrentRateMlPerHr(), iv.getDropSet()),
                iv.getDropSet(),
                iv.getStatus(),
                iv.getStartedAt(),
                iv.getExpectedEndAt(),
                secsLeft
        );
    }
}
