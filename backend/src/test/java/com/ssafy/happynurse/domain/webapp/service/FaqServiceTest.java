package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.FaqListResponse;
import com.ssafy.happynurse.domain.webapp.entity.Faq;
import com.ssafy.happynurse.domain.webapp.entity.FaqIntent;
import com.ssafy.happynurse.domain.webapp.repository.FaqRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock EncounterRepository encounterRepository;
    @Mock FaqRepository faqRepository;
    @Mock(lenient = true) DiseaseMatcher diseaseMatcher; // 일부 케이스에서 호출되지 않을 수 있음
    @Mock(lenient = true) IntentOrderingPolicy intentOrderingPolicy;

    @InjectMocks
    FaqService faqService;

    // 의도적으로 실제 정렬/매칭 로직을 검증하지 않고 의존 호출만 확인
    // (DiseaseMatcher/IntentOrderingPolicyTest에서 이미 로직 단위 검증함)

    @Test
    @DisplayName("정상: diseaseName 매칭 성공 → FAQ 리스트와 matchedFaqDisease 반환")
    void getFaq_success() {
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "퇴행성 무릎 관절염", "슬관절 전치환술", null);

        Faq faq1 = Faq.of("근골격질환", "퇴행성 관절염", "퇴행성관절염",
                FaqIntent.DEFINITION, "퇴행성 관절염은 ...", 100L);
        Faq faq2 = Faq.of("근골격질환", "퇴행성 관절염", "퇴행성관절염",
                FaqIntent.REHAB, "재활은 ...", 101L);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(diseaseMatcher.normalize("퇴행성 무릎 관절염")).willReturn("퇴행성무릎관절염");
        given(faqRepository.findCandidatesByPatientNorm("퇴행성무릎관절염"))
                .willReturn(List.of(faq1, faq2));
        given(diseaseMatcher.findBestMatch("퇴행성무릎관절염", List.of("퇴행성관절염", "퇴행성관절염")))
                .willReturn(Optional.of("퇴행성관절염"));
        given(intentOrderingPolicy.sort(List.of(FaqIntent.DEFINITION, FaqIntent.REHAB), true, false))
                .willReturn(List.of(FaqIntent.REHAB, FaqIntent.DEFINITION));

        FaqListResponse response = faqService.getFaq(1L, 1L);

        assertThat(response.getDiseaseName()).isEqualTo("퇴행성 무릎 관절염");
        assertThat(response.getMatchedFaqDisease()).isEqualTo("퇴행성 관절염");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getIntentLabel()).isEqualTo("재활");
        assertThat(response.getItems().get(0).getQuestion()).isEqualTo("재활은 어떻게 진행되나요?");
        assertThat(response.getItems().get(0).getAnswer()).isEqualTo("재활은 ...");
        assertThat(response.getItems().get(1).getIntentLabel()).isEqualTo("정의");
    }

    @Test
    @DisplayName("매칭 실패: 후보 없음 → 빈 items, matchedFaqDisease=null")
    void getFaq_noMatch_returnsEmpty() {
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, "복부 비만", null, null);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(diseaseMatcher.normalize("복부 비만")).willReturn("복부비만");
        given(faqRepository.findCandidatesByPatientNorm("복부비만"))
                .willReturn(List.of());
        given(diseaseMatcher.findBestMatch("복부비만", List.of()))
                .willReturn(Optional.empty());

        FaqListResponse response = faqService.getFaq(1L, 1L);

        assertThat(response.getDiseaseName()).isEqualTo("복부 비만");
        assertThat(response.getMatchedFaqDisease()).isNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("환자 diseaseName이 null → 빈 items, matchedFaqDisease=null")
    void getFaq_nullDiseaseName_returnsEmpty() {
        Patient patient = createPatient(1L);
        Encounter encounter = createEncounter(patient, null, null, null);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.of(encounter));
        given(diseaseMatcher.normalize(null)).willReturn("");
        given(faqRepository.findCandidatesByPatientNorm("")).willReturn(List.of());
        given(diseaseMatcher.findBestMatch("", List.of())).willReturn(Optional.empty());

        FaqListResponse response = faqService.getFaq(1L, 1L);

        assertThat(response.getDiseaseName()).isNull();
        assertThat(response.getMatchedFaqDisease()).isNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("JWT patientId ≠ path patientId → PATIENT_ID_MISMATCH")
    void getFaq_patientIdMismatch() {
        assertThatThrownBy(() -> faqService.getFaq(1L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_ID_MISMATCH);
    }

    @Test
    @DisplayName("환자 없음 → PATIENT_NOT_FOUND")
    void getFaq_patientNotFound() {
        given(patientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> faqService.getFaq(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 입원 없음 → ENCOUNTER_NOT_FOUND")
    void getFaq_encounterNotFound() {
        Patient patient = createPatient(1L);

        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));
        given(encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> faqService.getFaq(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    // ----- 헬퍼 -----

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

    private Encounter createEncounter(Patient patient, String diseaseName, String surgeryName, String chiefComplaint) {
        try {
            Room room = newInstance(Room.class);
            setField(room, "roomName", "301호실");

            Encounter encounter = newInstance(Encounter.class);
            setField(encounter, "patient", patient);
            setField(encounter, "name", "김가민");
            setField(encounter, "status", EncounterStatus.in_progress);
            setField(encounter, "room", room);
            setField(encounter, "diseaseName", diseaseName);
            setField(encounter, "surgeryName", surgeryName);
            setField(encounter, "chiefComplaint", chiefComplaint);
            return encounter;
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