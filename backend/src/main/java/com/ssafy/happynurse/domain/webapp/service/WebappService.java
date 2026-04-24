package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.NfcEntryResponse;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyRequest;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyResult;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebappService {

    private static final DateTimeFormatter BIRTH_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public NfcEntryResponse getPatientEntry(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        return new NfcEntryResponse(
                patientId,
                encounter.getName(),
                encounter.getRoom().getRoomName()
        );
    }

    public PatientVerifyResult verifyPatient(PatientVerifyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        boolean nameMatch = encounter.getName().equals(request.getName());
        boolean birthMatch = encounter.getBirthDate().format(BIRTH_FORMATTER).equals(request.getBirthDate());

        if (!nameMatch || !birthMatch) {
            throw new CustomException(ErrorCode.PATIENT_VERIFY_FAILED);
        }

        String token = jwtTokenProvider.createPatientToken(request.getPatientId(), encounter.getName());

        return new PatientVerifyResult(
                token,
                request.getPatientId(),
                encounter.getName(),
                encounter.getRoom().getRoomName()
        );
    }
}
