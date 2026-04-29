package com.ssafy.happynurse.domain.patient.dto;

import com.ssafy.happynurse.domain.patient.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record WardPatientListResponse(
        @Schema(description = "입원(Encounter) PK", example = "100")
        Long encounterId,

        @Schema(description = "환자명", example = "김가민")
        String name,

        @Schema(description = "성별", example = "female")
        Gender gender,

        @Schema(description = "생년월일", example = "1999-05-20")
        LocalDate birthDate,

        @Schema(description = "호실", example = "7101호")
        String roomName,

        @Schema(description = "침상", example = "A")
        String bedName,

        @Schema(description = "미확정 간호기록 개수 (status=draft)", example = "2")
        long unconfirmedNursingCount,

        @Schema(description = "현재 로그인 간호사의 담당 여부 (Redis 저장 기준, 시프트 교대 후에도 보존)", example = "true")
        boolean isMyPatient
) {
}
