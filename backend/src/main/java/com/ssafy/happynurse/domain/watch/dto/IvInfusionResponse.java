package com.ssafy.happynurse.domain.watch.dto;

import com.ssafy.happynurse.domain.watch.entity.DropSet;
import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.entity.IvInfusionMedication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "수액 상세 응답 — mix IV 의 약물들은 medications 배열에")
public record IvInfusionResponse(
        @Schema(description = "수액 PK", example = "42") Long ivInfusionId,
        @Schema(description = "환자 PK", example = "3") Long patientId,
        @Schema(description = "환자 이름", example = "이승연") String patientName,
        @Schema(description = "Encounter PK", example = "11") Long encounterId,
        @Schema(description = "처방 오더 PK (primary)", example = "12345") Long medicationOrderId,
        @Schema(description = "혼합된 약물들 (1개 이상)") List<MedicationItem> medications,
        @Schema(description = "투여 시작 간호사 PK", example = "7") Long practitionerId,
        @Schema(description = "총 용량 (mL)", example = "500.00") BigDecimal totalVolumeMl,
        @Schema(description = "현재 주입 속도 (mL/hr)", example = "100.00") BigDecimal currentRateMlPerHr,
        @Schema(description = "현재 주입 속도 (gtt/min) — dropSet 기반 역환산", example = "40") Integer rateGttPerMin,
        @Schema(description = "수액 세트 (gtt/mL): SET_10, SET_15, SET_20, SET_60", example = "SET_20") DropSet dropSet,
        @Schema(description = "투여 시작 시각") LocalDateTime startedAt,
        @Schema(description = "예상 종료 시각") LocalDateTime expectedEndAt,
        @Schema(description = "실제 종료 시각", nullable = true) LocalDateTime actualEndAt,
        @Schema(description = "상태", example = "IN_PROGRESS") InfusionStatus status,
        @Schema(description = "메모", nullable = true) String note,
        @Schema(description = "잔여 용량 (mL) — 응답 생성 시각 기준", example = "300.00") BigDecimal remainingVolumeMl,
        @Schema(description = "잔여 시간 (초) — 응답 생성 시각 기준", example = "10800") Long remainingSeconds
) {
    public record MedicationItem(
            @Schema(description = "약물 PK", example = "789") Long medicationId,
            @Schema(description = "약물명", example = "5% Dextrose") String medicationName,
            @Schema(description = "출처 처방 PK", example = "12345") Long medicationOrderId,
            @Schema(description = "표시 순서 (1-based)", example = "1") Integer sequence
    ) {
        public static MedicationItem from(IvInfusionMedication ivm) {
            return new MedicationItem(
                    ivm.getMedication().getMedicationId(),
                    ivm.getMedication().getProductName(),
                    ivm.getMedicationOrder().getMedicationOrderId(),
                    ivm.getSequence()
            );
        }
    }

    public static IvInfusionResponse from(IvInfusion iv, LocalDateTime now) {
        long secsLeft = Math.max(0L, Duration.between(now, iv.getExpectedEndAt()).getSeconds());
        List<MedicationItem> meds = iv.getMedications().stream()
                .map(MedicationItem::from)
                .toList();
        return new IvInfusionResponse(
                iv.getIvInfusionId(),
                iv.getPatient().getPatientId(),
                iv.getPatient().getName(),
                iv.getEncounter().getEncounterId(),
                iv.getMedicationOrder().getMedicationOrderId(),
                meds,
                iv.getPractitioner().getPractitionerId(),
                iv.getTotalVolumeMl(),
                iv.getCurrentRateMlPerHr(),
                RateInputResolver.toGttPerMin(iv.getCurrentRateMlPerHr(), iv.getDropSet()),
                iv.getDropSet(),
                iv.getStartedAt(),
                iv.getExpectedEndAt(),
                iv.getActualEndAt(),
                iv.getStatus(),
                iv.getNote(),
                iv.remainingVolumeMl(now),
                secsLeft
        );
    }
}
