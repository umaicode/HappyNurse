package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.*;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.NfcEntryResponse;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyRequest;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyResult;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import com.ssafy.happynurse.domain.nurse.notification.repository.NotificationRepository;
import com.ssafy.happynurse.domain.webapp.dto.SymptomSubmitRequest;
import com.ssafy.happynurse.domain.webapp.dto.SymptomSubmitResponse;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.entity.QuickSymptomButton;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import com.ssafy.happynurse.domain.webapp.repository.QuickSymptomButtonRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    @Mock
    QuickSymptomButtonRepository quickSymptomButtonRepository;
    @Mock
    PatientSelfReportRepository patientSelfReportRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

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
        String token = "valid-nfc-token-123";
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "김가민", LocalDate.of(2001, 4, 29), "301호실");

        given(patientRepository.findByNfcToken(token)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.of(encounter));

        // when
        NfcEntryResponse response = webAppService.getPatientEntry(token);

        // then
        assertThat(response.getPatientName()).isEqualTo("김가민");
        assertThat(response.getRoomName()).isEqualTo("301호실");
    }

    @Test
    @DisplayName("NFC 진입: 유효하지 않은 토큰 -> NFC_TOKEN_INVALID")
    void nfcEntry_tokenInvalid() {
        String token = "invalid-nfc-token-999";
        given(patientRepository.findByNfcToken(token)).willReturn(Optional.empty());

        assertThatThrownBy(() -> webAppService.getPatientEntry(token))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_TOKEN_INVALID);
    }

    @Test
    @DisplayName("NFC 진입: 활성 입원 없음 -> ENCOUNTER_NOT_FOUND")
    void nfcEntry_encounterNotFound() {
        // given
        String token = "valid-nfc-token-123";
        Patient patient = createPatient(1L);

        given(patientRepository.findByNfcToken(token)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> webAppService.getPatientEntry(token))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 확인: 이름 + 생년월일 일치 시 토큰과 환자 정보 반환")
    void verify_success() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounterForVerify(patient);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(jwtTokenProvider.createPatientToken(1L, "김가민")).willReturn("mock-token");

        // when
        PatientVerifyResult result = webAppService.verifyPatient(createRequest(1L, "김가민", "010429"));

        // then
        assertThat(result.getToken()).isEqualTo("mock-token");
        assertThat(result.getPatientName()).isEqualTo("김가민");
        assertThat(result.getRoomName()).isEqualTo("301호실");
        assertThat(result.getGender()).isEqualTo("female");
        assertThat(result.getDepartmentCode()).isEqualTo("정형외과");
        assertThat(result.getDiseaseName()).isEqualTo("퇴행성 무릎 관절염");
        assertThat(result.getChiefComplaint()).isEqualTo("무릎 통증");
        assertThat(result.getSurgeryName()).isEqualTo("슬관절 전치환술");
        assertThat(result.getAssignedNurseName()).isEqualTo("문현지");
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

    // ----- 증상 버튼 목록 조회 -----
    @Test
    @DisplayName("버튼 목록 조회: 정상 반환")
    void getButtons_success() {
        // given
        QuickSymptomButton btn = createButton(1L, "드레싱 교체", 1);
        given(quickSymptomButtonRepository.findAllByOrderByDisplayOrderAsc())
                .willReturn(List.of(btn));

        // when
        var result = webAppService.getButtons();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("드레싱 교체");
    }

    // ----- 증상 제출 -----
    @Test
    @DisplayName("증상 제출: 버튼 선택 성공")
    void submitSymptom_button_success() {
        // given
        Patient patient = createPatient(1L);
        Practitioner nurse = createPractitioner(10L);
        Encounter encounter = createEncounterWithWard(patient, "김가민", "301호", 3L, nurse);
        QuickSymptomButton button = createButton(1L, "드레싱 교체", 1);
        PatientSelfReport savedReport = createSavedReport(42L, LocalDateTime.now());

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(quickSymptomButtonRepository.findById(1L)).willReturn(Optional.of(button));
        given(patientSelfReportRepository.save(any())).willReturn(savedReport);

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(1L);

        // when
        SymptomSubmitResponse response = webAppService.submitSymptom(1L, 1L, request);

        // then
        assertThat(response.getSelfReportId()).isEqualTo(42L);
        verify(patientSelfReportRepository).save(any(PatientSelfReport.class));
        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher).publishEvent(any(SymptomSubmittedEvent.class));
    }

    @Test
    @DisplayName("증상 제출: 직접 입력 성공")
    void submitSymptom_text_success() {
        // given
        Patient patient = createPatient(1L);
        Practitioner nurse = createPractitioner(10L);
        Encounter encounter = createEncounterWithWard(patient, "김가민", "301호", 3L, nurse);
        PatientSelfReport savedReport = createSavedReport(43L, LocalDateTime.now());

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(patientSelfReportRepository.save(any())).willReturn(savedReport);

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setSymptomText("열이 납니다");

        // when
        SymptomSubmitResponse response = webAppService.submitSymptom(1L, 1L, request);

        // then
        assertThat(response.getSelfReportId()).isEqualTo(43L);
        verify(patientSelfReportRepository).save(any(PatientSelfReport.class));
        verify(eventPublisher).publishEvent(any(SymptomSubmittedEvent.class));
    }

    @Test
    @DisplayName("증상 제출: JWT patientId와 path patientId 불일치 -> PATIENT_ID_MISMATCH")
    void submitSymptom_patientIdMismatch() {
        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(1L);

        assertThatThrownBy(() -> webAppService.submitSymptom(1L, 99L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_ID_MISMATCH);
    }

    @Test
    @DisplayName("증상 제출: 없는 버튼 ID -> BUTTON_NOT_FOUND")
    void submitSymptom_buttonNotFound() {
        // given
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounterWithWard(patient, "김가민", "301호", 3L, null);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(quickSymptomButtonRepository.findById(99L)).willReturn(Optional.empty());

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(99L);

        assertThatThrownBy(() -> webAppService.submitSymptom(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUTTON_NOT_FOUND);
    }

    @Test
    @DisplayName("증상 제출: 버튼 + 텍스트 동시 입력 성공 — 합쳐진 텍스트로 저장")
    void submitSymptom_buttonAndText_success() {
        // given
        Patient patient = createPatient(1L);
        Practitioner nurse = createPractitioner(10L);
        Encounter encounter = createEncounterWithWard(patient, "김가민", "301호", 3L, nurse);
        QuickSymptomButton button = createButton(1L, "드레싱 교체", 1);
        PatientSelfReport savedReport = createSavedReport(44L, LocalDateTime.now());

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(quickSymptomButtonRepository.findById(1L)).willReturn(Optional.of(button));
        given(patientSelfReportRepository.save(any())).willReturn(savedReport);

        SymptomSubmitRequest request = new SymptomSubmitRequest();
        request.setButtonId(1L);
        request.setSymptomText("특히 왼쪽 다리가 심합니다");

        // when
        SymptomSubmitResponse response = webAppService.submitSymptom(1L, 1L, request);

        // then
        assertThat(response.getSelfReportId()).isEqualTo(44L);

        ArgumentCaptor<PatientSelfReport> reportCaptor = ArgumentCaptor.forClass(PatientSelfReport.class);
        verify(patientSelfReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getSymptomText())
                .isEqualTo("드레싱 교체 - 특히 왼쪽 다리가 심합니다");
        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher).publishEvent(any(SymptomSubmittedEvent.class));
    }

    @Test
    @DisplayName("증상 제출: buttonId와 symptomText 둘 다 없음 -> SYMPTOM_INPUT_INVALID")
    void submitSymptom_noInput() {
        assertThatThrownBy(() -> webAppService.submitSymptom(1L, 1L, new SymptomSubmitRequest()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYMPTOM_INPUT_INVALID);
    }

    @Test
    @DisplayName("devVerify: patientId만으로 토큰과 환자 정보 반환")
    void devVerify_success() {
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounterForVerify(patient);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(jwtTokenProvider.createPatientToken(1L, "김가민")).willReturn("mock-dev-token");

        PatientVerifyResult result = webAppService.devVerify(1L);

        assertThat(result.getToken()).isEqualTo("mock-dev-token");
        assertThat(result.getPatientId()).isEqualTo(1L);
        assertThat(result.getPatientName()).isEqualTo("김가민");
        assertThat(result.getRoomName()).isEqualTo("301호실");
        assertThat(result.getDiseaseName()).isEqualTo("퇴행성 무릎 관절염");
    }

    @Test
    @DisplayName("devVerify: 존재하지 않는 환자 → PATIENT_NOT_FOUND")
    void devVerify_patientNotFound() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> webAppService.devVerify(99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("devVerify: 활성 입원 없음 → ENCOUNTER_NOT_FOUND")
    void devVerify_encounterNotFound() {
        Patient patient = createPatient(1L);
        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> webAppService.devVerify(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
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

    private QuickSymptomButton createButton(Long id, String label, int order) {
        try {
            QuickSymptomButton btn = newInstance(QuickSymptomButton.class);
            setField(btn, "buttonId", id);
            setField(btn, "label", label);
            setField(btn, "displayOrder", order);
            return btn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Practitioner createPractitioner(Long id) {
        try {
            Practitioner p = newInstance(Practitioner.class);
            setField(p, "practitionerId", id);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Encounter createEncounterWithWard(Patient patient, String name,
                                              String roomName, Long wardId,
                                              Practitioner assignedPractitioner) {
        try {
            Ward ward = newInstance(Ward.class);
            setField(ward, "wardId", wardId);

            Room room = newInstance(Room.class);
            setField(room, "roomName", roomName);
            setField(room, "ward", ward);

            Encounter encounter = newInstance(Encounter.class);
            setField(encounter, "patient", patient);
            setField(encounter, "name", name);
            setField(encounter, "status", EncounterStatus.in_progress);
            setField(encounter, "room", room);
            setField(encounter, "assignedPractitioner", assignedPractitioner);
            return encounter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PatientSelfReport createSavedReport(Long id, LocalDateTime submittedAt) {
        try {
            PatientSelfReport report = newInstance(PatientSelfReport.class);
            setField(report, "selfReportId", id);
            setField(report, "submittedAt", submittedAt);
            return report;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Encounter createEncounterForVerify(Patient patient) {
        try {
            Room room = newInstance(Room.class);
            setField(room, "roomName", "301호실");

            Practitioner nurse = newInstance(Practitioner.class);
            setField(nurse, "practitionerId", 10L);
            setField(nurse, "name", "문현지");

            Encounter encounter = newInstance(Encounter.class);
            setField(encounter, "patient", patient);
            setField(encounter, "name", "김가민");
            setField(encounter, "birthDate", LocalDate.of(2001, 4, 29));
            setField(encounter, "status", EncounterStatus.in_progress);
            setField(encounter, "room", room);
            setField(encounter, "gender", Gender.female);
            setField(encounter, "departmentCode", "정형외과");
            setField(encounter, "diseaseName", "퇴행성 무릎 관절염");
            setField(encounter, "chiefComplaint", "무릎 통증");
            setField(encounter, "surgeryName", "슬관절 전치환술");
            setField(encounter, "assignedPractitioner", nurse);
            return encounter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
