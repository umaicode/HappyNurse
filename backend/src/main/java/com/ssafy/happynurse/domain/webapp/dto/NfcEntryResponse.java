package com.ssafy.happynurse.domain.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NfcEntryResponse {

    private Long patientId;
    private String patientName;
    private String roomName;
}
