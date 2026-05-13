package com.ssafy.happynurse.domain.handover.dto;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "오늘 우리 병동의 입퇴원 환자 목록")
public record WardEventsResponse(
        @Schema(description = "오늘 입원한 환자 목록 (periodStart 오름차순)")
        List<AdmissionItem> admissions,

        @Schema(description = "오늘 퇴원한 환자 목록 (periodEnd 오름차순)")
        List<DischargeItem> discharges
) {

    public record AdmissionItem(
            @Schema(example = "42") Long encounterId,
            @Schema(example = "홍길동") String patientName,
            @Schema(example = "501호") String roomName,
            @Schema(example = "1") String bedName,
            @Schema(description = "입원 경로", example = "응급") String classCode,
            @Schema(description = "주 증상", example = "복통") String chiefComplaint,
            @Schema(description = "병명", example = "급성 충수염") String diseaseName,
            @Schema(description = "수술명", example = "충수절제술") String surgeryName,
            @Schema(example = "2026-05-13T09:15:00") LocalDateTime periodStart
    ) {
        public static AdmissionItem from(Encounter e) {
            return new AdmissionItem(
                    e.getEncounterId(),
                    e.getName(),
                    e.getRoom().getRoomName(),
                    e.getBedName(),
                    e.getClassCode() == null ? null : e.getClassCode().getLabel(),
                    e.getChiefComplaint(),
                    e.getDiseaseName(),
                    e.getSurgeryName(),
                    e.getPeriodStart());
        }
    }

    public record DischargeItem(
            @Schema(example = "37") Long encounterId,
            @Schema(example = "김영희") String patientName,
            @Schema(example = "502호") String roomName,
            @Schema(example = "3") String bedName,
            @Schema(description = "입원 경로", example = "응급") String classCode,
            @Schema(description = "주 증상", example = "기침/발열") String chiefComplaint,
            @Schema(description = "병명", example = "폐렴") String diseaseName,
            @Schema(description = "수술명", example = "기관지 내시경") String surgeryName,
            @Schema(example = "2026-05-13T14:20:00") LocalDateTime periodEnd
    ) {
        public static DischargeItem from(Encounter e) {
            return new DischargeItem(
                    e.getEncounterId(),
                    e.getName(),
                    e.getRoom().getRoomName(),
                    e.getBedName(),
                    e.getClassCode() == null ? null : e.getClassCode().getLabel(),
                    e.getChiefComplaint(),
                    e.getDiseaseName(),
                    e.getSurgeryName(),
                    e.getPeriodEnd());
        }
    }
}
