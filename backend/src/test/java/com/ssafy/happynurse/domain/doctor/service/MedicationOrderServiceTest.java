package com.ssafy.happynurse.domain.doctor.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.entity.OrderType;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationOrderServiceTest {

    @Mock
    MedicationOrderRepository medicationOrderRepository;
    @Mock
    PatientRepository patientRepository;
    @InjectMocks
    MedicationOrderService medicationOrderService;

    @Test
    @DisplayName("환자별 오더 조회 성공 시 오더 목록을 반환한다")
    void getOrdersByPatientId_성공() {
        // Given
        Patient patient = createPatient(3L, "이승연");
        Practitioner prescriber = createPractitioner(6L, "이조은");
        MedicationOrder order = createOrder(9L, patient, prescriber,
                OrderType.FLUID, OrderStatus.active, "IV5001", "5% Dextrose Inj. 1L",
                new BigDecimal("1000"), 1, "bag", "IV",
                LocalDateTime.of(2026, 4, 27, 14, 0));

        LocalDate date = LocalDate.of(2026, 4, 27);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(medicationOrderRepository.findByPatientIdAndDate(3L, dayStart, dayEnd))
                .willReturn(List.of(order));

        // When
        MedicationOrderListResponse response = medicationOrderService.getOrdersByPatientId(3L, date);

        // Then
        assertThat(response.patientId()).isEqualTo(3L);
        assertThat(response.patientName()).isEqualTo("이승연");
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.orders()).hasSize(1);
        assertThat(response.orders().get(0).medicationOrderId()).isEqualTo(9L);
        assertThat(response.orders().get(0).orderType()).isEqualTo(OrderType.FLUID);
        assertThat(response.orders().get(0).prescriberName()).isEqualTo("이조은");
    }

    @Test
    @DisplayName("존재하지 않는 환자 → PATIENT_NOT_FOUND")
    void getOrdersByPatientId_실패_환자_없음() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicationOrderService.getOrdersByPatientId(99L, LocalDate.now()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("해당 날짜에 오더가 없으면 빈 목록을 반환한다")
    void getOrdersByPatientId_빈_목록() {
        // Given
        Patient patient = createPatient(3L, "이승연");
        LocalDate date = LocalDate.of(2026, 5, 1);

        given(patientRepository.findById(3L)).willReturn(Optional.of(patient));
        given(medicationOrderRepository.findByPatientIdAndDate(eq(3L), any(), any()))
                .willReturn(List.of());

        // When
        MedicationOrderListResponse response = medicationOrderService.getOrdersByPatientId(3L, date);

        // Then
        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.orders()).isEmpty();
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

    private Practitioner createPractitioner(Long id, String name) {
        try {
            Practitioner p = newInstance(Practitioner.class);
            setField(p, "practitionerId", id);
            setField(p, "name", name);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MedicationOrder createOrder(Long id, Patient patient, Practitioner prescriber,
                                        OrderType type, OrderStatus status,
                                        String code, String name,
                                        BigDecimal dose, Integer freq, String unit, String route,
                                        LocalDateTime dateWritten) {
        try {
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
            return mo;
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