package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "[개발용] 환자 본인 확인 응답 — accessToken 포함")
public record DevPatientVerifyResponse(
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbG...")
        String accessToken,

        @Schema(description = "환자 ID", example = "1")
        Long patientId,

        @Schema(description = "환자 이름", example = "김가민")
        String patientName,

        @Schema(description = "현재 입원 병실", example = "301호실")
        String roomName,

        @Schema(description = "성별 (male/female/other/unknown)", example = "female")
        String gender,

        @Schema(description = "진료 부서", example = "정형외과")
        String departmentCode,

        @Schema(description = "병명", example = "퇴행성 무릎 관절염")
        String diseaseName,

        @Schema(description = "주증상", example = "무릎 통증")
        String chiefComplaint,

        @Schema(description = "수술명 (수술 없는 환자는 null)", example = "슬관절 전치환술")
        String surgeryName,

        @Schema(description = "담당 간호사 이름", example = "문현지")
        String assignedNurseName
) {
    public static DevPatientVerifyResponse from(PatientVerifyResult result) {
        return new DevPatientVerifyResponse(
                result.getToken(),
                result.getPatientId(),
                result.getPatientName(),
                result.getRoomName(),
                result.getGender(),
                result.getDepartmentCode(),
                result.getDiseaseName(),
                result.getChiefComplaint(),
                result.getSurgeryName(),
                result.getAssignedNurseName()
        );
    }
}