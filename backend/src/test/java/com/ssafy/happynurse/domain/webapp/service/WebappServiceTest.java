package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.NfcEntryResponse;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyRequest;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyResult;
import com.ssafy.happynurse.global.exception.CustomException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class WebappServiceTest {

    @Mock
    PatientRepository patientRepository;
    @Mock
    EncounterRepository encounterRepository;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @InjectMocks
    WebappService webAppService;

    @Test
    @DisplayName("존재하지 않는 환자 진입 시 PATIENT_NOT_FOUND 발생")
    void placeholder() {
        // 에러 코드가 존재하는지 확인
        ErrorCode code = ErrorCode.PATIENT_NOT_FOUND;
    }

    @Test
    @DisplayName("NFC 진입: 정상 조회 시 환자 이름과 병실 반환")
    void nfcEntry_success() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "김가민", LocalDate.of(2001, 4, 29), "301호실");

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.of(encounter));

        // when
        NfcEntryResponse response = webAppService.getPatientEntry(1L);

        // then
        assertThat(response.getPatientName()).isEqualTo("김가민");
        assertThat(response.getRoomName()).isEqualTo("301호실");
    }

    @Test
    @DisplayName("NFC 진입: 존재하지 않는 환자 -> PATIENT_NOT_FOUND")
    void nfcEntry_patientNotFound() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> webAppService.getPatientEntry(99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("NFC 진입: 활성 입원 없음 -> ENCOUNTER_NOT_FOUND")
    void nfcEntry_encounterNotFound() {
        // given
        Patient patient = createPatient(1L);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> webAppService.getPatientEntry(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 확인: 이름 + 생년월일 일치 시 토큰과 환자 정보 반환")
    void verify_succes() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "김가민", LocalDate.of(2001, 4, 29), "301호실");

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.of(encounter));
        given(jwtTokenProvider.createPatientToken(1L, "김가민")).willReturn("mock-token");

        // when
        PatientVerifyResult result = webAppService.verifyPatient(createRequest(1L, "김가민", "010429"));

        // then
        assertThat(result.getToken()).isEqualTo("mock-token");
        assertThat(result.getPatientName()).isEqualTo("김가민");
        assertThat(result.getRoomName()).isEqualTo("301호실");
    }

    @Test
    @DisplayName("본인 확인: 이름 불일치 -> PATIENT_VERIFY_FAILED")
    void verify_nameMismatch() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "김가민", LocalDate.of(2001, 4, 29), "301호실");

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.of(encounter));

        // then
        assertThatThrownBy(() -> webAppService.verifyPatient(createRequest(1L, "이다른", "010429")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_VERIFY_FAILED);
    }

    @Test
    @DisplayName("본인 확인: 생년월일 불일치 -> PATIENT_VERIFY_FAILED")
    void verify_birthDateMismatch() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "김가민", LocalDate.of(2001, 4, 29), "301호실");

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));

        // then
        assertThatThrownBy(() -> webAppService.verifyPatient(createRequest(1L, "김가민", "990101")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_VERIFY_FAILED);
    }

    // 헬퍼 함수
    private Patient createPatient(Long id) {
        try {
            Patient patient = newInstance(Patient.class);
            setField(patient, "patientId", id);
            setField(patient, "active", true);
            return patient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Encounter createEncounter(Patient patient, String name, LocalDate birthDate, String roomName) {
        try {
            Room room = newInstance(Room.class);
            setField(room, "roomName", roomName);

            Encounter encounter = newInstance(Encounter.class);
            setField(encounter, "patient", patient);
            setField(encounter, "name", name);
            setField(encounter, "birthDate", birthDate);
            setField(encounter, "status", EncounterStatus.in_progress);
            setField(encounter, "room", room);
            return encounter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PatientVerifyRequest createRequest(Long patientId, String name, String birthDate) {
        PatientVerifyRequest request = new PatientVerifyRequest();
        request.setPatientId(patientId);
        request.setName(name);
        request.setBirthDate(birthDate);
        return request;
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

}
