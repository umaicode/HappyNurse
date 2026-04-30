package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.patient.dto.SymptomReportItemResponse;
import com.ssafy.happynurse.domain.patient.dto.SymptomReportListResponse;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SymptomReportService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PatientSelfReportRepository patientSelfReportRepository;

    public SymptomReportListResponse getSymptomsByPatientId(Long patientId, Long wardId, LocalDate date) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        if (!encounter.getRoom().getWard().getWardId().equals(wardId)) {
            throw new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<PatientSelfReport> reports = patientSelfReportRepository
                .findByPatientIdAndDate(patientId, dayStart, dayEnd);

        List<SymptomReportItemResponse> items = reports.stream()
                .map(this::toItemResponse)
                .toList();

        return new SymptomReportListResponse(
                patient.getPatientId(),
                patient.getName(),
                items.size(),
                items
        );
    }

    private SymptomReportItemResponse toItemResponse(PatientSelfReport report) {
        return new SymptomReportItemResponse(
                report.getSelfReportId(),
                report.getInputMethod(),
                report.getQuickSymptomButton() != null ? report.getQuickSymptomButton().getLabel() : null,
                report.getSymptomText(),
                report.getSubmittedAt()
        );
    }
}
