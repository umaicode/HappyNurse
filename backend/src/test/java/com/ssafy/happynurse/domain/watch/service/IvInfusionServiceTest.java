package com.ssafy.happynurse.domain.watch.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.watch.dto.ChangeRateRequest;
import com.ssafy.happynurse.domain.watch.dto.IvInfusionResponse;
import com.ssafy.happynurse.domain.watch.dto.StartIvRequest;
import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.domain.watch.entity.DropSet;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import com.ssafy.happynurse.domain.watch.scheduler.AlertType;
import com.ssafy.happynurse.domain.watch.scheduler.IvAlertScheduler;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IvInfusionServiceTest {

    @Mock IvInfusionRepository repository;
    @Mock NfcTagRepository nfcTagRepository;
    @Mock MedicationOrderRepository medicationOrderRepository;
    @Mock EncounterRepository encounterRepository;
    @Mock PractitionerRepository practitionerRepository;
    @Mock IvAlertScheduler scheduler;

    @InjectMocks IvInfusionService service;

    private static final String TAG_UID = "04:69:C1:48:C6:2A:81";
    private static final Long ENCOUNTER_ID = 11L;
    private static final Long PRIMARY_ORDER_ID = 12345L;
    private static final Long SECOND_ORDER_ID = 12346L;
    private static final Long PRACTITIONER_ID = 7L;

    // ---------- start ----------

    @Test
    @DisplayName("start - 정상 단일: encounter + 1개 order → 1개 medication 으로 IV 생성, scheduler 등록")
    void start_단일_정상() {
        Patient patient = stub(Patient.class, "patientId", 3L);
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        Medication med = stub(Medication.class, "medicationId", 789L);
        MedicationOrder order = orderWithMedication(PRIMARY_ORDER_ID, patient, encounter, med);
        Practitioner nurse = stub(Practitioner.class, "practitionerId", PRACTITIONER_ID);

        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findAllById(List.of(PRIMARY_ORDER_ID))).willReturn(List.of(order));
        given(repository.existsByMedicationOrder_MedicationOrderIdAndStatus(PRIMARY_ORDER_ID, InfusionStatus.IN_PROGRESS))
                .willReturn(false);
        given(practitionerRepository.findById(PRACTITIONER_ID)).willReturn(Optional.of(nurse));
        given(repository.save(any(IvInfusion.class))).willAnswer(inv -> {
            IvInfusion iv = inv.getArgument(0);
            setField(iv, "ivInfusionId", 100L);
            return iv;
        });

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID, List.of(PRIMARY_ORDER_ID),
                new BigDecimal("500"), 33, DropSet.SET_20, "메모");

        IvInfusionResponse response = service.start(req, PRACTITIONER_ID);

        assertThat(response.ivInfusionId()).isEqualTo(100L);
        assertThat(response.medications()).hasSize(1);
        assertThat(response.medications().get(0).medicationId()).isEqualTo(789L);

        verify(scheduler).register(any(IvInfusion.class), eq(AlertType.FIVE_MIN_BEFORE));
        verify(scheduler).register(any(IvInfusion.class), eq(AlertType.COMPLETED));
    }

    @Test
    @DisplayName("start - mix: order 2개 → medications 2개로 components 생성, primary = 첫 order")
    void start_mix_복수_orders() {
        Patient patient = stub(Patient.class, "patientId", 3L);
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        Medication med1 = stub(Medication.class, "medicationId", 789L);
        Medication med2 = stub(Medication.class, "medicationId", 901L);
        MedicationOrder o1 = orderWithMedication(PRIMARY_ORDER_ID, patient, encounter, med1);
        MedicationOrder o2 = orderWithMedication(SECOND_ORDER_ID, patient, encounter, med2);
        Practitioner nurse = stub(Practitioner.class, "practitionerId", PRACTITIONER_ID);

        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findAllById(List.of(PRIMARY_ORDER_ID, SECOND_ORDER_ID)))
                .willReturn(List.of(o1, o2));
        given(repository.existsByMedicationOrder_MedicationOrderIdAndStatus(any(), eq(InfusionStatus.IN_PROGRESS)))
                .willReturn(false);
        given(practitionerRepository.findById(PRACTITIONER_ID)).willReturn(Optional.of(nurse));
        given(repository.save(any(IvInfusion.class))).willAnswer(inv -> inv.getArgument(0));

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID,
                List.of(PRIMARY_ORDER_ID, SECOND_ORDER_ID),
                new BigDecimal("500"), 33, DropSet.SET_20, null);

        IvInfusionResponse response = service.start(req, PRACTITIONER_ID);

        assertThat(response.medications()).hasSize(2);
        assertThat(response.medications()).extracting("medicationId").containsExactly(789L, 901L);
        assertThat(response.medicationOrderId()).isEqualTo(PRIMARY_ORDER_ID); // primary = 첫 order
    }

    @Test
    @DisplayName("start - encounter 미존재 → ENCOUNTER_NOT_FOUND")
    void start_encounter_없음() {
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.empty());

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID, List.of(PRIMARY_ORDER_ID),
                new BigDecimal("500"), 33, DropSet.SET_20, null);

        assertThatThrownBy(() -> service.start(req, PRACTITIONER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);

        verify(repository, never()).save(any());
        verify(scheduler, never()).register(any(), any());
    }

    @Test
    @DisplayName("start - medicationOrderIds 중 일부 없음 → MEDICATION_ORDER_NOT_FOUND")
    void start_order_일부_없음() {
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        // 2개 요청했는데 1개만 반환
        Medication med = stub(Medication.class, "medicationId", 789L);
        Patient patient = stub(Patient.class, "patientId", 3L);
        MedicationOrder o1 = orderWithMedication(PRIMARY_ORDER_ID, patient, encounter, med);
        given(medicationOrderRepository.findAllById(List.of(PRIMARY_ORDER_ID, 99999L)))
                .willReturn(List.of(o1));

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID,
                List.of(PRIMARY_ORDER_ID, 99999L),
                new BigDecimal("500"), 33, DropSet.SET_20, null);

        assertThatThrownBy(() -> service.start(req, PRACTITIONER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ORDER_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("start - order 의 encounter 가 요청 encounterId 와 다르면 MEDICATION_ORDER_PATIENT_MISMATCH")
    void start_order_encounter_불일치() {
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        Encounter otherEncounter = stub(Encounter.class, "encounterId", 999L);
        Medication med = stub(Medication.class, "medicationId", 789L);
        Patient patient = stub(Patient.class, "patientId", 3L);
        MedicationOrder mismatch = orderWithMedication(PRIMARY_ORDER_ID, patient, otherEncounter, med);

        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findAllById(List.of(PRIMARY_ORDER_ID))).willReturn(List.of(mismatch));

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID, List.of(PRIMARY_ORDER_ID),
                new BigDecimal("500"), 33, DropSet.SET_20, null);

        assertThatThrownBy(() -> service.start(req, PRACTITIONER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ORDER_PATIENT_MISMATCH);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("start - orders 중 하나라도 IN_PROGRESS IV 있으면 IV_ALREADY_IN_PROGRESS 409")
    void start_중복_거절() {
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        Patient patient = stub(Patient.class, "patientId", 3L);
        Medication med1 = stub(Medication.class, "medicationId", 789L);
        Medication med2 = stub(Medication.class, "medicationId", 901L);
        MedicationOrder o1 = orderWithMedication(PRIMARY_ORDER_ID, patient, encounter, med1);
        MedicationOrder o2 = orderWithMedication(SECOND_ORDER_ID, patient, encounter, med2);

        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findAllById(List.of(PRIMARY_ORDER_ID, SECOND_ORDER_ID)))
                .willReturn(List.of(o1, o2));
        // 첫 order 는 자유, 두 번째 order 가 다른 IV 에서 진행 중
        given(repository.existsByMedicationOrder_MedicationOrderIdAndStatus(PRIMARY_ORDER_ID, InfusionStatus.IN_PROGRESS))
                .willReturn(false);
        given(repository.existsByMedicationOrder_MedicationOrderIdAndStatus(SECOND_ORDER_ID, InfusionStatus.IN_PROGRESS))
                .willReturn(true);

        StartIvRequest req = new StartIvRequest(ENCOUNTER_ID,
                List.of(PRIMARY_ORDER_ID, SECOND_ORDER_ID),
                new BigDecimal("500"), 33, DropSet.SET_20, null);

        assertThatThrownBy(() -> service.start(req, PRACTITIONER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_ALREADY_IN_PROGRESS);

        verify(repository, never()).save(any());
        verify(scheduler, never()).register(any(), any());
    }

    // ---------- changeRateByTag ----------

    @Test
    @DisplayName("changeRateByTag - tag → IV 해석 후 entity.changeRate, cancelAll + 양쪽 재등록")
    void changeRate_정상() {
        IvInfusion iv = realIv(50L, PRIMARY_ORDER_ID);
        given(nfcTagRepository.findByTagUidAndIsActiveTrue(TAG_UID))
                .willReturn(Optional.of(nfcOrderTag(TAG_UID, PRIMARY_ORDER_ID)));
        given(repository.findActiveByMedicationOrderIdWithRoutingInfo(PRIMARY_ORDER_ID))
                .willReturn(Optional.of(iv));

        ChangeRateRequest req = new ChangeRateRequest(60, DropSet.SET_20);
        service.changeRateByTag(TAG_UID, req);

        verify(scheduler).cancelAll(50L);
        verify(scheduler).register(iv, AlertType.FIVE_MIN_BEFORE);
        verify(scheduler).register(iv, AlertType.COMPLETED);
    }

    @Test
    @DisplayName("changeRateByTag - NFC 태그 미등록 → NFC_TAG_NOT_FOUND")
    void changeRate_태그없음() {
        given(nfcTagRepository.findByTagUidAndIsActiveTrue(TAG_UID)).willReturn(Optional.empty());

        ChangeRateRequest req = new ChangeRateRequest(60, DropSet.SET_20);

        assertThatThrownBy(() -> service.changeRateByTag(TAG_UID, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NFC_TAG_NOT_FOUND);

        verify(scheduler, never()).cancelAll(any());
    }

    @Test
    @DisplayName("changeRateByTag - 진행 중 IV 없으면 IV_INFUSION_NOT_FOUND")
    void changeRate_IV없음() {
        given(nfcTagRepository.findByTagUidAndIsActiveTrue(TAG_UID))
                .willReturn(Optional.of(nfcOrderTag(TAG_UID, PRIMARY_ORDER_ID)));
        given(repository.findActiveByMedicationOrderIdWithRoutingInfo(PRIMARY_ORDER_ID))
                .willReturn(Optional.empty());

        ChangeRateRequest req = new ChangeRateRequest(60, DropSet.SET_20);

        assertThatThrownBy(() -> service.changeRateByTag(TAG_UID, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IV_INFUSION_NOT_FOUND);
    }

    // ---------- completeByTag ----------

    @Test
    @DisplayName("completeByTag - 정상: status=COMPLETED, scheduler.cancelAll")
    void complete_정상() {
        IvInfusion iv = realIv(50L, PRIMARY_ORDER_ID);
        given(nfcTagRepository.findByTagUidAndIsActiveTrue(TAG_UID))
                .willReturn(Optional.of(nfcOrderTag(TAG_UID, PRIMARY_ORDER_ID)));
        given(repository.findActiveByMedicationOrderIdWithRoutingInfo(PRIMARY_ORDER_ID))
                .willReturn(Optional.of(iv));

        IvInfusionResponse response = service.completeByTag(TAG_UID);

        assertThat(response.status()).isEqualTo(InfusionStatus.COMPLETED);
        assertThat(iv.getActualEndAt()).isNotNull();
        verify(scheduler).cancelAll(50L);
    }

    // ---------- helpers ----------

    private static MedicationOrder orderWithMedication(Long id, Patient p, Encounter e, Medication m) {
        MedicationOrder o = newInstance(MedicationOrder.class);
        setField(o, "medicationOrderId", id);
        setField(o, "patient", p);
        setField(o, "encounter", e);
        setField(o, "medication", m);
        return o;
    }

    private static NfcTag nfcOrderTag(String tagUid, Long orderId) {
        NfcTag tag = newInstance(NfcTag.class);
        setField(tag, "tagUid", tagUid);
        setField(tag, "tagType", TagType.medication);
        setField(tag, "isActive", true);
        setField(tag, "payloadJson", new NfcPayload("ORDER", orderId));
        return tag;
    }

    /** patient/encounter/order/medication 다 채운 실제 IvInfusion (entity 비즈니스 메서드 호출용). */
    private IvInfusion realIv(Long id, Long orderId) {
        Patient patient = stub(Patient.class, "patientId", 3L);
        Encounter encounter = stub(Encounter.class, "encounterId", ENCOUNTER_ID);
        Medication med = stub(Medication.class, "medicationId", 789L);
        MedicationOrder order = orderWithMedication(orderId, patient, encounter, med);
        IvInfusion iv = IvInfusion.start(
                patient, encounter,
                List.of(order),
                stub(Practitioner.class, "practitionerId", PRACTITIONER_ID),
                new BigDecimal("500"), new BigDecimal("100"),
                DropSet.SET_20,
                LocalDateTime.now(), null);
        setField(iv, "ivInfusionId", id);
        return iv;
    }

    private static <T> T stub(Class<T> clazz, String idField, Long idValue) {
        T obj = newInstance(clazz);
        setField(obj, idField, idValue);
        return obj;
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

    @SuppressWarnings("unused")
    private static void unusedLenient() { lenient(); }
}
