package com.ssafy.happynurse.domain.patient.dto;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "환자 정보 응답")
public record PatientResonse(
        @Schema(description = "환자 ID") Long patientId,
        @Schema(description = "환자 등록번호 (MRN)") String identifierValue,
        @Schema(description = "이름") String name,
        @Schema(description = "성별") String gender,
        @Schema(description = "생년월일") LocalDate birthDate,
        @Schema(description = "연락처") String phone,
        @Schema(description = "주소") String address,
        @Schema(description = "Encounter ID") Long encounterId,
        @Schema(description = "내원 상태") String status,
        @Schema(description = "입원 시각") LocalDateTime periodStart,
        @Schema(description = "병명") String diseaseName,
        @Schema(description = "주 증상") String chiefComplaint,
        @Schema(description = "수술명") String surgeryName,
        @Schema(description = "진료부서 코드") String departmentCode,
        @Schema(description = "병동명") String wardName,
        @Schema(description = "병실명") String roomName,
        @Schema(description = "침상명") String bedName
) {
    public static PatientResonse of(Patient patient, Encounter encounter) {
        return new PatientResonse(
                patient.getPatientId(),
                patient.getIdentifierValue(),
                patient.getName(),
                patient.getGender().name(),
                patient.getBirthDate(),
                patient.getPhone(),
                patient.getAddress(),
                encounter.getEncounterId(),
                encounter.getStatus().name(),
                encounter.getPeriodStart(),
                encounter.getDiseaseName(),
                encounter.getChiefComplaint(),
                encounter.getSurgeryName(),
                encounter.getDepartmentCode(),
                encounter.getRoom().getWard().getWardName(),
                encounter.getRoom().getRoomName(),
                encounter.getBedName()
        );
    }
}
