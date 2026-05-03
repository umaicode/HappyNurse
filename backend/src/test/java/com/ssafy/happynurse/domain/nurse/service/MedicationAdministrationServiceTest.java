package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationUpdateRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationWriteResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationDosageUpdateRequest;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.watch.entity.Medication;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicationAdministrationServiceTest {

    @Mock
    MedicationAdministrationRepository medicationAdministrationRepository;
    @InjectMocks
    MedicationAdministrationService medicationAdministrationService;

    private static final String TAG = "tag-uuid-1";
    private static final Long ME = 6L;
    private static final Long OTHER = 99L;

    @Test
    @DisplayName("draft 그룹 확정 → 모든 row status=confirmed로 일괄 갱신")
    void confirm_성공_그룹_일괄() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        LocalDateTime t = LocalDateTime.of(2026, 5, 3, 14, 25);

        List<MedicationAdministration> draftGroup = List.of(
                createAdmin(31L, nurse, m, TAG, t, RecordStatus.draft, true),
                createAdmin(32L, nurse, m, TAG, t, RecordStatus.draft, true)
        );
        List<MedicationAdministration> confirmedGroup = List.of(
                createAdmin(31L, nurse, m, TAG, t, RecordStatus.confirmed, true),
                createAdmin(32L, nurse, m, TAG, t, RecordStatus.confirmed, true)
        );
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(draftGroup)
                .willReturn(confirmedGroup);

        MedicationAdministrationWriteResponse response = medicationAdministrationService.confirm(TAG, ME);

        verify(medicationAdministrationRepository).confirmByTaggingId(TAG);
        assertThat(response.taggingId()).isEqualTo(TAG);
        assertThat(response.status()).isEqualTo(RecordStatus.confirmed);
        assertThat(response.medications()).hasSize(2);
    }

    @Test
    @DisplayName("그룹 없음 → MEDICATION_ADMIN_NOT_FOUND")
    void confirm_실패_없음() {
        given(medicationAdministrationRepository.findAllByTaggingId(TAG)).willReturn(List.of());

        assertThatThrownBy(() -> medicationAdministrationService.confirm(TAG, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 투약 → MEDICATION_ADMIN_NOT_AUTHOR")
    void confirm_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, other, m, TAG, LocalDateTime.now(),
                        RecordStatus.draft, true)));

        assertThatThrownBy(() -> medicationAdministrationService.confirm(TAG, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR);
    }

    @Test
    @DisplayName("draft가 아닌 그룹 확정 시도 → INVALID_RECORD_STATUS")
    void confirm_실패_draft_아님() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, nurse, m, TAG, LocalDateTime.now(),
                        RecordStatus.confirmed, true)));

        assertThatThrownBy(() -> medicationAdministrationService.confirm(TAG, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RECORD_STATUS);
    }

    @Test
    @DisplayName("effectiveDatetime만 수정 → 그룹 일괄 갱신")
    void update_성공_시각만() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        LocalDateTime original = LocalDateTime.of(2026, 5, 3, 14, 0);
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 3, 16, 0);

        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, nurse, m, TAG, original, RecordStatus.draft, true)))
                .willReturn(List.of(createAdmin(31L, nurse, m, TAG, newTime, RecordStatus.draft, true)));

        MedicationAdministrationWriteResponse response = medicationAdministrationService.update(
                TAG, new MedicationAdministrationUpdateRequest(newTime, null), ME);

        verify(medicationAdministrationRepository).updateEffectiveDatetimeByTaggingId(TAG, newTime);
        verify(medicationAdministrationRepository, never()).updateDosage(anyLong(), any(), anyString());
        assertThat(response.effectiveDatetime()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("dosage만 수정 → 단건씩 갱신")
    void update_성공_dosage만() {
        Practitioner nurse = createPractitioner(ME);
        Medication m1 = createMedication(101L, "PC1", "약A");
        Medication m2 = createMedication(102L, "PC2", "약B");

        List<MedicationAdministration> group = List.of(
                createAdmin(31L, nurse, m1, TAG, LocalDateTime.now(), RecordStatus.draft, true),
                createAdmin(32L, nurse, m2, TAG, LocalDateTime.now(), RecordStatus.draft, true)
        );
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(group)
                .willReturn(group);

        MedicationAdministrationUpdateRequest request = new MedicationAdministrationUpdateRequest(
                null,
                List.of(
                        new MedicationDosageUpdateRequest(31L, new BigDecimal("1.500"), "tab"),
                        new MedicationDosageUpdateRequest(32L, new BigDecimal("500"), "mL")
                )
        );

        medicationAdministrationService.update(TAG, request, ME);

        verify(medicationAdministrationRepository).updateDosage(31L, new BigDecimal("1.500"), "tab");
        verify(medicationAdministrationRepository).updateDosage(32L, new BigDecimal("500"), "mL");
        verify(medicationAdministrationRepository, never()).updateEffectiveDatetimeByTaggingId(anyString(), any());
    }

    @Test
    @DisplayName("effectiveDatetime + dosage 둘 다 수정")
    void update_성공_둘다() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 3, 16, 0);

        List<MedicationAdministration> group = List.of(
                createAdmin(31L, nurse, m, TAG, LocalDateTime.now(), RecordStatus.draft, true)
        );
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(group)
                .willReturn(group);

        MedicationAdministrationUpdateRequest request = new MedicationAdministrationUpdateRequest(
                newTime,
                List.of(new MedicationDosageUpdateRequest(31L, new BigDecimal("2.000"), "mg"))
        );

        medicationAdministrationService.update(TAG, request, ME);

        verify(medicationAdministrationRepository).updateEffectiveDatetimeByTaggingId(TAG, newTime);
        verify(medicationAdministrationRepository).updateDosage(31L, new BigDecimal("2.000"), "mg");
    }

    @Test
    @DisplayName("그룹 외 medicationAdminId 포함 → INVALID_INPUT_VALUE")
    void update_실패_그룹_외_medicationAdminId() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, nurse, m, TAG, LocalDateTime.now(),
                        RecordStatus.draft, true)));

        MedicationAdministrationUpdateRequest request = new MedicationAdministrationUpdateRequest(
                null,
                List.of(new MedicationDosageUpdateRequest(999L, new BigDecimal("1.0"), "mg"))
        );

        assertThatThrownBy(() -> medicationAdministrationService.update(TAG, request, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(medicationAdministrationRepository, never()).updateDosage(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("update 시 그룹 없음 → MEDICATION_ADMIN_NOT_FOUND")
    void update_실패_없음() {
        given(medicationAdministrationRepository.findAllByTaggingId(TAG)).willReturn(List.of());

        assertThatThrownBy(() -> medicationAdministrationService.update(
                TAG, new MedicationAdministrationUpdateRequest(LocalDateTime.now(), null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("update 시 타인 투약 → MEDICATION_ADMIN_NOT_AUTHOR")
    void update_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, other, m, TAG, LocalDateTime.now(),
                        RecordStatus.draft, true)));

        assertThatThrownBy(() -> medicationAdministrationService.update(
                TAG, new MedicationAdministrationUpdateRequest(LocalDateTime.now(), null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR);
    }

    @Test
    @DisplayName("delete 성공 → 그룹 통째 삭제")
    void delete_성공_그룹_통째() {
        Practitioner nurse = createPractitioner(ME);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, nurse, m, TAG, LocalDateTime.now(),
                        RecordStatus.draft, true)));

        medicationAdministrationService.delete(TAG, ME);

        verify(medicationAdministrationRepository, times(1)).deleteByTaggingId(TAG);
    }

    @Test
    @DisplayName("delete 시 그룹 없음 → MEDICATION_ADMIN_NOT_FOUND")
    void delete_실패_없음() {
        given(medicationAdministrationRepository.findAllByTaggingId(TAG)).willReturn(List.of());

        assertThatThrownBy(() -> medicationAdministrationService.delete(TAG, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("delete 시 타인 투약 → MEDICATION_ADMIN_NOT_AUTHOR")
    void delete_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        Medication m = createMedication(101L, "PC1", "약A");
        given(medicationAdministrationRepository.findAllByTaggingId(TAG))
                .willReturn(List.of(createAdmin(31L, other, m, TAG, LocalDateTime.now(),
                        RecordStatus.draft, true)));

        assertThatThrownBy(() -> medicationAdministrationService.delete(TAG, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR);
    }

    // --- 헬퍼 ---

    private Practitioner createPractitioner(Long id) {
        Practitioner p = newInstance(Practitioner.class);
        setField(p, "practitionerId", id);
        setField(p, "name", "이조은");
        return p;
    }

    private Medication createMedication(Long id, String productCode, String productName) {
        Medication m = newInstance(Medication.class);
        setField(m, "medicationId", id);
        setField(m, "productCode", productCode);
        setField(m, "productName", productName);
        return m;
    }

    private MedicationAdministration createAdmin(Long id, Practitioner practitioner, Medication medication,
                                                 String taggingId, LocalDateTime effectiveDatetime,
                                                 RecordStatus status, boolean nfcTagVerified) {
        MedicationAdministration ma = newInstance(MedicationAdministration.class);
        setField(ma, "medicationAdminId", id);
        setField(ma, "practitioner", practitioner);
        setField(ma, "medication", medication);
        setField(ma, "taggingId", taggingId);
        setField(ma, "effectiveDatetime", effectiveDatetime);
        setField(ma, "status", status);
        setField(ma, "nfcTagVerified", nfcTagVerified);
        setField(ma, "dosageQuantity", new BigDecimal("1.000"));
        setField(ma, "dosageUnit", "mg");
        return ma;
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
