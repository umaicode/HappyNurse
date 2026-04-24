package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "NFC 진입 응답")
@Getter
@AllArgsConstructor
public class NfcEntryResponse {

    @Schema(description = "환자 ID", example = "1")
    private Long patientId;

    @Schema(description = "환자 이름", example = "김가민")
    private String patientName;

    @Schema(description = "현재 입원 병실", example = "301호실")
    private String roomName;
}
