package com.ssafy.happynurse.domain.nfc.service;

import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NfcService {

    private final PatientRepository patientRepository;

    public record NfcPatientInfo(Long patientId, String name) {}

    public NfcPatientInfo resolveToken(String token) {
        Patient patient = patientRepository.findByNfcToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.NFC_TOKEN_INVALID));
        return new NfcPatientInfo(patient.getPatientId(), patient.getName());
    }
}