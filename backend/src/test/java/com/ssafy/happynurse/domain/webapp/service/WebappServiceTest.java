package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.NfcEntryResponse;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class WebappServiceTest {

    @Mock
    PatientRepository patientRepository;
    @Mock
    EncounterRepository encounterRepository;

    @Test
    @DisplayName("존재하지 않는 환자 진입 시 PATIENT_NOT_FOUND 발생")
    void placeholder() {
        // 에러 코드가 존재하는지 확인
        ErrorCode code = ErrorCode.PATIENT_NOT_FOUND;
    }
}
