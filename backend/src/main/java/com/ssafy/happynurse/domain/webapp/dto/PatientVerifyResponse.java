package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "본인 확인 응답 (JWT는 HttpOnly Cookie로 별도 전달)")
@Getter
@AllArgsConstructor
public class PatientVerifyResponse {

    @Schema(description = "환자 ID", example = "1")
    private Long patientId;

    @Schema(description = "환자 이름", example = "김가민")
    private String patientName;

    @Schema(description = "현재 입원 병실", example = "301호실")
    private String roomName;

    @Schema(description = "성별 (male/female/other/unknown)", example = "female")
    private String gender;

    @Schema(description = "진료 부서", example = "정형외과")
    private String departmentCode;

    @Schema(description = "병명", example = "퇴행성 무릎 관절염")
    private String diseaseName;

    @Schema(description = "주증상", example = "무릎 통증")
    private String chiefComplaint;

    @Schema(description = "수술명 (수술 없는 환자는 null)", example = "슬관절 전치환술")
    private String surgeryName;

    @Schema(description = "담당 간호사 이름", example = "문현지")
    private String assignedNurseName;
}
