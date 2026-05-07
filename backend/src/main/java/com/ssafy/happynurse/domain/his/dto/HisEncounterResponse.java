package com.ssafy.happynurse.domain.his.dto;

public record HisEncounterResponse(
        Long encounterId,
        Long patientId,
        String patientName,
        String roomName,
        String bedName
) {}
