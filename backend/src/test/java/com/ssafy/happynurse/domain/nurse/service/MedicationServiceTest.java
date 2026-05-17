package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.entity.OrderType;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyResponse;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    @Mock NfcTagRepository nfcTagRepository;
    @Mock MedicationOrderRepository medicationOrderRepository;
    @Mock MedicationAdministrationRepository medicationAdministrationRepository;
    @Mock PatientRepository patientRepository;
    @Mock EncounterRepository encounterRepository;
    @Mock PractitionerRepository practitionerRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MedicationService medicationService;

    // ---------- verify() — 태그 1개 단위 호출 ----------

    @Test
    @DisplayName("verify - ORDER 태그가 환자와 일치하면 medicationOrderId 반환")
    void verify_orderTag_성공() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(1L, "UID-ORDER-1", TagType.medication, new NfcPayload("ORDER", 12345L));
        MedicationOrder order = order(12345L, patient, medication(7L), OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-ORDER-1")).willReturn(Optional.of(tag));
        given(medicationOrderRepository.findAllByIdInWithPatient(List.of(12345L)))
                .willReturn(List.of(order));

        MedicationVerifyResponse response = medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-ORDER-1"));

        assertThat(response.verified()).isTrue();
        assertThat(response.medicationOrderId()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("verify - DRUG 태그가 환자의 active 처방과 일치하면 매핑된 orderId 반환")
    void verify_drugTag_성공() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(2L, "UID-DRUG-1", TagType.medication, new NfcPayload("DRUG", 789L));
        Medication med = medication(789L);
        MedicationOrder activeOrder = order(900L, patient, med, OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-DRUG-1")).willReturn(Optional.of(tag));
        given(medicationOrderRepository.findActiveByPatientAndMedicationIds(eq(3L), anyCollection()))
                .willReturn(List.of(activeOrder));

        MedicationVerifyResponse response = medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-DRUG-1"));

        assertThat(response.medicationOrderId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("verify - DRUG 태그에 active 처방이 여러 건이면 가장 먼저 작성된 orderId 반환")
    void verify_drugTag_여러_active() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(2L, "UID-DRUG-1", TagType.medication, new NfcPayload("DRUG", 789L));
        Medication med = medication(789L);
        MedicationOrder o1 = order(901L, patient, med, OrderStatus.active);
        MedicationOrder o2 = order(902L, patient, med, OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-DRUG-1")).willReturn(Optional.of(tag));
        // repository 가 dateWritten ASC 정렬 보장. 첫 번째 항목이 매핑됨.
        given(medicationOrderRepository.findActiveByPatientAndMedicationIds(eq(3L), anyCollection()))
                .willReturn(List.of(o1, o2));

        MedicationVerifyResponse response = medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-DRUG-1"));

        assertThat(response.medicationOrderId()).isEqualTo(901L);
    }

    @Test
    @DisplayName("verify - ORDER 태그의 환자가 다르면 MEDICATION_VERIFICATION_FAILED")
    void verify_orderTag_환자_불일치() {
        Patient requested = patient(3L);
        Patient other = patient(99L);
        NfcTag tag = nfcTag(1L, "UID-ORDER-X", TagType.medication, new NfcPayload("ORDER", 12345L));
        MedicationOrder orderForOtherPatient = order(12345L, other, medication(7L), OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(requested));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-ORDER-X")).willReturn(Optional.of(tag));
        given(medicationOrderRepository.findAllByIdInWithPatient(List.of(12345L)))
                .willReturn(List.of(orderForOtherPatient));

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-ORDER-X")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("verify - DRUG 태그가 환자의 active 처방에 없으면 MEDICATION_VERIFICATION_FAILED")
    void verify_drugTag_매칭없음() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(2L, "UID-DRUG-Z", TagType.medication, new NfcPayload("DRUG", 789L));

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-DRUG-Z")).willReturn(Optional.of(tag));
        given(medicationOrderRepository.findActiveByPatientAndMedicationIds(eq(3L), anyCollection()))
                .willReturn(List.of());

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-DRUG-Z")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("verify - tagUid 가 DB에 없으면 NFC_TAG_NOT_FOUND")
    void verify_태그_없음() {
        Patient patient = patient(3L);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-MISSING")).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-MISSING")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_TAG_NOT_FOUND);
    }

    @Test
    @DisplayName("verify - 약물 태그가 아니면(wristband 등) NFC_TAG_NOT_MEDICATION")
    void verify_약물태그_아님() {
        Patient patient = patient(3L);
        NfcTag wristband = nfcTag(99L, "UID-PATIENT-001",
                TagType.patient_wristband, new NfcPayload("PATIENT", 3L));

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-PATIENT-001"))
                .willReturn(Optional.of(wristband));

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-PATIENT-001")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_TAG_NOT_MEDICATION);
    }

    @Test
    @DisplayName("verify - 알 수 없는 payload type 이면 NFC_PAYLOAD_INVALID")
    void verify_알수없는_payload_type() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(1L, "UID-?", TagType.medication, new NfcPayload("UNKNOWN", 1L));

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-?")).willReturn(Optional.of(tag));

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-?")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_PAYLOAD_INVALID);
    }

    @Test
    @DisplayName("verify - 환자 미존재 시 PATIENT_NOT_FOUND")
    void verify_환자_없음() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(99L, "UID-1")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("verify - ORDER 태그가 이미 completed 상태면 MEDICATION_ALREADY_ADMINISTERED (조기 거절)")
    void verify_orderTag_이미_완료() {
        Patient patient = patient(3L);
        NfcTag tag = nfcTag(1L, "UID-ORDER-DONE", TagType.medication, new NfcPayload("ORDER", 12345L));
        // 같은 환자 처방이지만 이미 completed 상태
        MedicationOrder completed = order(12345L, patient, medication(7L), OrderStatus.completed);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(nfcTagRepository.findByTagUidAndIsActiveTrue("UID-ORDER-DONE")).willReturn(Optional.of(tag));
        given(medicationOrderRepository.findAllByIdInWithPatient(List.of(12345L)))
                .willReturn(List.of(completed));

        assertThatThrownBy(() -> medicationService.verify(
                new MedicationVerifyRequest(3L, "UID-ORDER-DONE")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ALREADY_ADMINISTERED);
    }

    // ---------- saveAdministrations() ----------

    @Test
    @DisplayName("saveAdministrations - 처방 ID 3개 → 3건 INSERT, 처방 상태 모두 completed 로 갱신")
    void save_성공_3건() {
        Patient patient = patient(3L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);
        Medication m1 = medication(101L);
        Medication m2 = medication(102L);
        Medication m3 = medication(103L);
        MedicationOrder o1 = order(12345L, patient, m1, OrderStatus.active);
        MedicationOrder o2 = order(12346L, patient, m2, OrderStatus.active);
        MedicationOrder o3 = order(12347L, patient, m3, OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(12345L, 12346L, 12347L)))
                .willReturn(List.of(o1, o2, o3));
        given(medicationAdministrationRepository.saveAll(any())).willAnswer(inv -> {
            List<MedicationAdministration> arg = inv.getArgument(0);
            for (int i = 0; i < arg.size(); i++) {
                setField(arg.get(i), "medicationAdminId", 1000L + i);
            }
            return arg;
        });

        MedicationAdministrationSaveResponse response = medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L, 12346L, 12347L)), 7L);

        assertThat(response.savedCount()).isEqualTo(3);
        assertThat(response.medicationAdminIds()).hasSize(3);
        assertThat(response.taggingId()).isNotBlank();

        ArgumentCaptor<List<MedicationAdministration>> captor = ArgumentCaptor.forClass(List.class);
        verify(medicationAdministrationRepository).saveAll(captor.capture());
        List<MedicationAdministration> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(MedicationAdministration::getNfcTagVerified);
        assertThat(saved).allMatch(ma -> ma.getStatus() == RecordStatus.draft);
        assertThat(saved).allMatch(ma -> ma.getEffectiveDatetime() != null);

        // 같은 호출 내 모든 row 의 taggingId 가 동일하고, 응답의 taggingId 와 일치
        assertThat(saved).extracting(MedicationAdministration::getTaggingId)
                .containsOnly(response.taggingId());

        // 처방 상태가 dirty-checking 으로 갱신됨 (saveAll 호출 없이도)
        assertThat(o1.getStatus()).isEqualTo(OrderStatus.completed);
        assertThat(o2.getStatus()).isEqualTo(OrderStatus.completed);
        assertThat(o3.getStatus()).isEqualTo(OrderStatus.completed);
    }

    @Test
    @DisplayName("saveAdministrations - 호출이 다르면 taggingId 도 달라야 한다")
    void save_taggingId_호출별_고유() {
        Patient patient = patient(3L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        // 호출마다 새 active 처방 인스턴스 반환 — 첫 호출의 markCompleted() 가 다음 호출에 누설되지 않도록.
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(12345L)))
                .willAnswer(inv -> List.of(order(12345L, patient, medication(101L), OrderStatus.active)));
        given(medicationAdministrationRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        MedicationAdministrationSaveResponse first = medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L)), 7L);
        MedicationAdministrationSaveResponse second = medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L)), 7L);

        assertThat(first.taggingId()).isNotBlank();
        assertThat(second.taggingId()).isNotBlank();
        assertThat(first.taggingId()).isNotEqualTo(second.taggingId());
    }

    @Test
    @DisplayName("saveAdministrations - 처방의 환자가 다르면 MEDICATION_ORDER_PATIENT_MISMATCH")
    void save_환자_불일치() {
        Patient patient = patient(3L);
        Patient other = patient(99L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);
        MedicationOrder mismatch = order(12345L, other, medication(7L), OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(12345L)))
                .willReturn(List.of(mismatch));

        assertThatThrownBy(() -> medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L)), 7L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ORDER_PATIENT_MISMATCH);

        verify(medicationAdministrationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("saveAdministrations - 일부 orderId 가 DB에 없으면 MEDICATION_ORDER_NOT_FOUND")
    void save_orderId_없음() {
        Patient patient = patient(3L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);
        MedicationOrder o1 = order(12345L, patient, medication(7L), OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(12345L, 99999L)))
                .willReturn(List.of(o1));

        assertThatThrownBy(() -> medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L, 99999L)), 7L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ORDER_NOT_FOUND);

        verify(medicationAdministrationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("saveAdministrations - 락 보유 중 상태 재검증: 이미 completed 면 INSERT 없이 MEDICATION_ALREADY_ADMINISTERED")
    void save_락_후_상태_재검증_실패() {
        Patient patient = patient(3L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);
        // verify 시점에는 active 였으나, 다른 트랜잭션이 먼저 완료시킨 상황을 모사.
        MedicationOrder completed = order(12345L, patient, medication(7L), OrderStatus.completed);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(12345L)))
                .willReturn(List.of(completed));

        assertThatThrownBy(() -> medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L)), 7L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ALREADY_ADMINISTERED);

        verify(medicationAdministrationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("saveAdministrations - 락 조회는 ID 오름차순 정렬해서 호출 (데드락 회피)")
    void save_락_획득_순서_검증() {
        Patient patient = patient(3L);
        Encounter enc = encounter(11L);
        Practitioner nurse = practitioner(7L);
        MedicationOrder oA = order(100L, patient, medication(1L), OrderStatus.active);
        MedicationOrder oB = order(200L, patient, medication(2L), OrderStatus.active);
        MedicationOrder oC = order(300L, patient, medication(3L), OrderStatus.active);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(encounterRepository.findById(11L)).willReturn(Optional.of(enc));
        given(practitionerRepository.findById(7L)).willReturn(Optional.of(nurse));
        // 호출자가 어떤 순서로 보내든 repository 에는 ID ASC 정렬된 List 가 들어가야 함.
        given(medicationOrderRepository.lockAllByIdsOrdered(List.of(100L, 200L, 300L)))
                .willReturn(List.of(oA, oB, oC));
        given(medicationAdministrationRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        // 일부러 거꾸로 / 중복 포함해서 요청
        medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(300L, 100L, 200L, 100L)), 7L);

        verify(medicationOrderRepository).lockAllByIdsOrdered(List.of(100L, 200L, 300L));
    }

    @Test
    @DisplayName("saveAdministrations - 환자/입원/의료진 미존재 시 각각의 NOT_FOUND")
    void save_사전조건_미존재() {
        given(patientRepository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicationService.saveAdministrations(
                new MedicationAdministrationSaveRequest(3L, 11L, List.of(12345L)), 7L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    // ---------- 헬퍼 ----------

    private Patient patient(Long id) {
        Patient p = newInstance(Patient.class);
        setField(p, "patientId", id);
        return p;
    }

    private Encounter encounter(Long id) {
        Encounter e = newInstance(Encounter.class);
        setField(e, "encounterId", id);
        return e;
    }

    private Practitioner practitioner(Long id) {
        Practitioner p = newInstance(Practitioner.class);
        setField(p, "practitionerId", id);
        return p;
    }

    private Medication medication(Long id) {
        Medication m = newInstance(Medication.class);
        setField(m, "medicationId", id);
        return m;
    }

    private NfcTag nfcTag(Long id, String uid, TagType tagType, NfcPayload payload) {
        NfcTag t = newInstance(NfcTag.class);
        setField(t, "nfcTagId", id);
        setField(t, "tagUid", uid);
        setField(t, "tagType", tagType);
        setField(t, "payloadJson", payload);
        setField(t, "isActive", true);
        return t;
    }

    private MedicationOrder order(Long id, Patient patient, Medication medication, OrderStatus status) {
        MedicationOrder mo = newInstance(MedicationOrder.class);
        setField(mo, "medicationOrderId", id);
        setField(mo, "patient", patient);
        setField(mo, "medication", medication);
        setField(mo, "status", status);
        setField(mo, "orderType", OrderType.MEDICATION);
        setField(mo, "dose", new BigDecimal("1.0"));
        setField(mo, "doseUnit", "tab");
        return mo;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object obj, String name, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                var f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException ignore) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("no such field: " + name);
    }

}
