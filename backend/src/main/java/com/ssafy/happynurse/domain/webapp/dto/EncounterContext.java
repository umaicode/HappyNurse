package com.ssafy.happynurse.domain.webapp.dto;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public record EncounterContext(
        @Nullable String departmentCode,
        @Nullable String surgeryName,
        @Nullable String diseaseName,
        @Nullable String chiefComplaint,
        @Nullable Integer age,
        @Nullable String gender,
        @Nullable Integer podDays
) {

    public static EncounterContext from(Encounter encounter) {
        if (encounter == null) {
            return empty();
        }
        LocalDate today = LocalDate.now();
        Integer age = encounter.getBirthDate() != null
                ? Period.between(encounter.getBirthDate(), today).getYears()
                : null;
        Integer podDays = encounter.getPeriodStart() != null
                ? (int) ChronoUnit.DAYS.between(encounter.getPeriodStart().toLocalDate(), today)
                : null;
        return new EncounterContext(
                encounter.getDepartmentCode(),
                encounter.getSurgeryName(),
                encounter.getDiseaseName(),
                encounter.getChiefComplaint(),
                age,
                encounter.getGender() != null ? encounter.getGender().name() : null,
                podDays
        );
    }

    public static EncounterContext ofDepartment(@Nullable String departmentCode) {
        return new EncounterContext(departmentCode, null, null, null, null, null, null);
    }

    public static EncounterContext empty() {
        return new EncounterContext(null, null, null, null, null, null, null);
    }
}
