package com.ssafy.happynurse.domain.doctor.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.entity.OrderType;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MedicationOrderServiceTest {

    @Mock
    MedicationOrderRepository medicationOrderRepository;
    @Mock
    EncounterRepository encounterRepository;
    @InjectMocks
    MedicationOrderService medicationOrderService;

    @Test
    @DisplayName("입원별 오더 조회 성공 시 encounter/patient 정보와 createdAt·updatedAt이 포함된다")
    void getOrdersByEncounterId_성공() {
        Patient patient = createPatient(3L, "이승연");
        Encounter encounter = createEncounter(42L, patient);
        Practitioner prescriber = createPractitioner(6L, "이조은");
        LocalDateTime created = LocalDateTime.of(2026, 4, 27, 14, 0, 5);
        LocalDateTime updated = LocalDateTime.of(2026, 4, 27, 15, 30, 12);
        MedicationOrder order = createOrder(9L, patient, prescriber,
                OrderType.FLUID, OrderStatus.active, "IV5001", "5% Dextrose Inj. 1L",
                new BigDecimal("1000"), 1, "bag", "IV",
                LocalDateTime.of(2026, 4, 27, 14, 0),
                created, updated);

        given(encounterRepository.findById(42L)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findByEncounterId(42L))
                .willReturn(List.of(order));

        MedicationOrderListResponse response = medicationOrderService.getOrdersByEncounterId(42L);

        assertThat(response.encounterId()).isEqualTo(42L);
        assertThat(response.patientId()).isEqualTo(3L);
        assertThat(response.patientName()).isEqualTo("이승연");
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.orders()).hasSize(1);
        assertThat(response.orders().get(0).medicationOrderId()).isEqualTo(9L);
        assertThat(response.orders().get(0).orderType()).isEqualTo(OrderType.FLUID);
        assertThat(response.orders().get(0).prescriberName()).isEqualTo("이조은");
        assertThat(response.orders().get(0).createdAt()).isEqualTo(created);
        assertThat(response.orders().get(0).updatedAt()).isEqualTo(updated);
    }

    @Test
    @DisplayName("존재하지 않는 입원 → ENCOUNTER_NOT_FOUND")
    void getOrdersByEncounterId_실패_입원_없음() {
        given(encounterRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicationOrderService.getOrdersByEncounterId(99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    @Test
    @DisplayName("입원에 오더가 없으면 빈 목록을 반환한다")
    void getOrdersByEncounterId_빈_목록() {
        Patient patient = createPatient(3L, "이승연");
        Encounter encounter = createEncounter(42L, patient);

        given(encounterRepository.findById(42L)).willReturn(Optional.of(encounter));
        given(medicationOrderRepository.findByEncounterId(42L)).willReturn(List.of());

        MedicationOrderListResponse response = medicationOrderService.getOrdersByEncounterId(42L);

        assertThat(response.encounterId()).isEqualTo(42L);
        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.orders()).isEmpty();
    }

    // --- 헬퍼 ---

    private Patient createPatient(Long id, String name) {
        Patient p = newInstance(Patient.class);
        setField(p, "patientId", id);
        setField(p, "name", name);
        return p;
    }

    private Encounter createEncounter(Long id, Patient patient) {
        Encounter e = newInstance(Encounter.class);
        setField(e, "encounterId", id);
        setField(e, "patient", patient);
        return e;
    }

    private Practitioner createPractitioner(Long id, String name) {
        Practitioner p = newInstance(Practitioner.class);
        setField(p, "practitionerId", id);
        setField(p, "name", name);
        return p;
    }

    private MedicationOrder createOrder(Long id, Patient patient, Practitioner prescriber,
                                        OrderType type, OrderStatus status,
                                        String code, String name,
                                        BigDecimal dose, Integer freq, String unit, String route,
                                        LocalDateTime dateWritten,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        MedicationOrder mo = newInstance(MedicationOrder.class);
        setField(mo, "medicationOrderId", id);
        setField(mo, "patient", patient);
        setField(mo, "prescriber", prescriber);
        setField(mo, "orderType", type);
        setField(mo, "status", status);
        setField(mo, "orderCode", code);
        setField(mo, "orderName", name);
        setField(mo, "dose", dose);
        setField(mo, "frequency", freq);
        setField(mo, "doseUnit", unit);
        setField(mo, "route", route);
        setField(mo, "dateWritten", dateWritten);
        setField(mo, "createdAt", createdAt);
        setField(mo, "updatedAt", updatedAt);
        return mo;
    }

    private <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(fieldName));
    }
}