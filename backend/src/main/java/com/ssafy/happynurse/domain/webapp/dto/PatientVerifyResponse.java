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
}
