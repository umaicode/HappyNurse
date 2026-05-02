package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.patient.dto.SymptomReportListResponse;
import com.ssafy.happynurse.domain.patient.entity.*;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.entity.InputMethod;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.entity.QuickSymptomButton;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SymptomReportServiceTest {

    @Mock
    PatientRepository patientRepository;
    @Mock
    EncounterRepository encounterRepository;
    @Mock
    PatientSelfReportRepository patientSelfReportRepository;
    @InjectMocks
    SymptomReportService symptomReportService;

    static final Long WARD_ID = 3L;
    static final Long PATIENT_ID = 1L;
    static final LocalDate DATE = LocalDate.of(2026, 4, 29);

    @Test
    @DisplayName("증상 보고 조회 성공 시 목록을 반환한다")
    void getSymptomsByPatientId_성공() {
        // Given
        Patient patient = createPatient(PATIENT_ID, "이승연");
        Encounter encounter = createEncounterWithWard(patient, WARD_ID);
        QuickSymptomButton button = createButton(1L, "통증");
        PatientSelfReport report = createReport(1L, InputMethod.quick_button, button, "통증",
                LocalDateTime.of(2026, 4, 29, 10, 30));

        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(patientSelfReportRepository.findByPatientIdAndDate(eq(PATIENT_ID), any(), any()))
                .willReturn(List.of(report));

        // When
        SymptomReportListResponse response = symptomReportService.getSymptomsByPatientId(PATIENT_ID, WARD_ID, DATE);

        // Then
        assertThat(response.patientId()).isEqualTo(PATIENT_ID);
        assertThat(response.patientName()).isEqualTo("이승연");
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.symptoms().get(0).buttonLabel()).isEqualTo("통증");
        assertThat(response.symptoms().get(0).inputMethod()).isEqualTo(InputMethod.quick_button);
    }

    @Test
    @DisplayName("텍스트 직접 입력 증상 - buttonLabel은 null")
    void getSymptomsByPatientId_텍스트_입력() {
        // Given
        Patient patient = createPatient(PATIENT_ID, "이승연");
        Encounter encounter = createEncounterWithWard(patient, WARD_ID);
        PatientSelfReport report = createReport(2L, InputMethod.text, null, "복부 불편감",
                LocalDateTime.of(2026, 4, 29, 14, 0));

        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(patientSelfReportRepository.findByPatientIdAndDate(eq(PATIENT_ID), any(), any()))
                .willReturn(List.of(report));

        // When
        SymptomReportListResponse response = symptomReportService.getSymptomsByPatientId(PATIENT_ID, WARD_ID, DATE);

        // Then
        assertThat(response.symptoms().get(0).buttonLabel()).isNull();
        assertThat(response.symptoms().get(0).symptomText()).isEqualTo("복부 불편감");
    }

    @Test
    @DisplayName("존재하지 않는 환자 → PATIENT_NOT_FOUND")
    void getSymptomsByPatientId_실패_환자_없음() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> symptomReportService.getSymptomsByPatientId(99L, WARD_ID, DATE))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 입원 정보 없음 → ENCOUNTER_NOT_FOUND")
    void getSymptomsByPatientId_실패_입원_없음() {
        Patient patient = createPatient(PATIENT_ID, "이승연");

        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> symptomReportService.getSymptomsByPatientId(PATIENT_ID, WARD_ID, DATE))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 병동 환자 조회 → ENCOUNTER_NOT_IN_MY_WARD")
    void getSymptomsByPatientId_실패_다른_병동() {
        Patient patient = createPatient(PATIENT_ID, "이승연");
        Encounter encounter = createEncounterWithWard(patient, 999L); // 다른 병동

        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));

        assertThatThrownBy(() -> symptomReportService.getSymptomsByPatientId(PATIENT_ID, WARD_ID, DATE))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
    }

    @Test
    @DisplayName("해당 날짜에 증상 보고가 없으면 빈 목록을 반환한다")
    void getSymptomsByPatientId_빈_목록() {
        Patient patient = createPatient(PATIENT_ID, "이승연");
        Encounter encounter = createEncounterWithWard(patient, WARD_ID);

        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(patientSelfReportRepository.findByPatientIdAndDate(eq(PATIENT_ID), any(), any()))
                .willReturn(List.of());

        SymptomReportListResponse response = symptomReportService.getSymptomsByPatientId(PATIENT_ID, WARD_ID, DATE);

        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.symptoms()).isEmpty();
    }

    // --- 헬퍼 ---

    private Patient createPatient(Long id, String name) {
        try {
            Patient p = newInstance(Patient.class);
            setField(p, "patientId", id);
            setField(p, "name", name);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Encounter createEncounterWithWard(Patient patient, Long wardId) {
        try {
            Ward ward = newInstance(Ward.class);
            setField(ward, "wardId", wardId);

            Room room = newInstance(Room.class);
            setField(room, "ward", ward);

            Encounter encounter = newInstance(Encounter.class);
            setField(encounter, "patient", patient);
            setField(encounter, "status", EncounterStatus.in_progress);
            setField(encounter, "room", room);
            return encounter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QuickSymptomButton createButton(Long id, String label) {
        try {
            QuickSymptomButton btn = newInstance(QuickSymptomButton.class);
            setField(btn, "buttonId", id);
            setField(btn, "label", label);
            return btn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PatientSelfReport createReport(Long id, InputMethod method, QuickSymptomButton button,
                                           String text, LocalDateTime submittedAt) {
        try {
            PatientSelfReport r = newInstance(PatientSelfReport.class);
            setField(r, "selfReportId", id);
            setField(r, "inputMethod", method);
            setField(r, "quickSymptomButton", button);
            setField(r, "symptomText", text);
            setField(r, "submittedAt", submittedAt);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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