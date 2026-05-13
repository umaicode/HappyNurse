package com.ssafy.happynurse.domain.his.dto;

import com.ssafy.happynurse.domain.patient.entity.ClassCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HisEncounterCreateRequest(
        @NotNull Long patientId,
        @NotNull ClassCode classCode,
        @NotNull Long roomId,
        @NotBlank String bedName,
        Long attendingPhysicianId,
        Long assignedPractitionerId,
        String departmentCode,
        String diseaseName,
        String chiefComplaint,
        String surgeryName
) {}
