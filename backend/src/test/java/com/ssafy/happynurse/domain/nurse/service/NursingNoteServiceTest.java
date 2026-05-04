package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemType;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NursingNoteServiceTest {

    @Mock
    EncounterRepository encounterRepository;
    @Mock
    NursingRecordRepository nursingRecordRepository;
    @Mock
    MedicationAdministrationRepository medicationAdministrationRepository;
    @Mock
    PractitionerRepository practitionerRepository;
    @InjectMocks
    NursingNoteService nursingNoteService;

    private static final Long ENCOUNTER_ID = 42L;
    private static final Long WARD_ID = 7L;
    private static final Long ME = 6L;
    private static final Long OTHER = 99L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 3);
    private static final LocalDateTime DAY_START = DATE.atStartOfDay();
    private static final LocalDateTime DAY_END = DATE.plusDays(1).atStartOfDay();

    @Test
    @DisplayName("두 종류 모두 비어있으면 빈 리스트를 반환한다")
    void getNursingNotes_성공_빈_결과() {
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 입원 → ENCOUNTER_NOT_FOUND")
    void getNursingNotes_실패_입원_없음() {
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 병동 외 입원 → ENCOUNTER_NOT_IN_MY_WARD")
    void getNursingNotes_실패_타_병동() {
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, 999L)));

        assertThatThrownBy(() -> nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
    }

    @Test
    @DisplayName("STT/MED가 섞이면 occurredAt 기준 DESC로 정렬된다")
    void getNursingNotes_성공_정렬_DESC() {
        Practitioner nurse = createPractitioner(ME, "이조은");
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));

        NursingRecord older = createNursingRecord(11L, nurse, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 5));
        NursingRecord newer = createNursingRecord(12L, nurse, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 14, 0),
                LocalDateTime.of(2026, 5, 3, 14, 5));
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(older, newer));

        Medication med = createMedication(101L, "PC1", "약A");
        MedicationAdministration ma = createAdmin(31L, nurse, med, null,
                "tag-uuid-1", LocalDateTime.of(2026, 5, 3, 12, 0),
                RecordStatus.confirmed, true);
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(ma));

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).type()).isEqualTo(NursingNoteItemType.STT_NOTE);
        assertThat(result.get(0).nursingRecordId()).isEqualTo(12L);
        assertThat(result.get(1).type()).isEqualTo(NursingNoteItemType.MEDICATION);
        assertThat(result.get(2).type()).isEqualTo(NursingNoteItemType.STT_NOTE);
        assertThat(result.get(2).nursingRecordId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("같은 taggingId의 투약 row 3개는 1개 항목 + medications 3개로 묶인다")
    void getNursingNotes_성공_투약_그룹핑() {
        Practitioner nurse = createPractitioner(ME, "이조은");
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());

        Medication m1 = createMedication(101L, "PC1", "약A");
        Medication m2 = createMedication(102L, "PC2", "약B");
        Medication m3 = createMedication(103L, "PC3", "약C");
        String tag = "tag-uuid-shared";
        LocalDateTime t = LocalDateTime.of(2026, 5, 3, 14, 25);

        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(
                        createAdmin(31L, nurse, m1, null, tag, t, RecordStatus.confirmed, true),
                        createAdmin(32L, nurse, m2, null, tag, t, RecordStatus.confirmed, true),
                        createAdmin(33L, nurse, m3, null, tag, t, RecordStatus.confirmed, true)
                ));

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(1);
        NursingNoteItemResponse item = result.get(0);
        assertThat(item.type()).isEqualTo(NursingNoteItemType.MEDICATION);
        assertThat(item.taggingId()).isEqualTo(tag);
        assertThat(item.medications()).hasSize(3);
        assertThat(item.medications()).extracting("productCode").containsExactly("PC1", "PC2", "PC3");
        assertThat(item.editable()).isTrue();
    }

    @Test
    @DisplayName("작성자 == 현재 유저면 editable=true, 아니면 false")
    void getNursingNotes_성공_editable_분기() {
        Practitioner me = createPractitioner(ME, "이조은");
        Practitioner other = createPractitioner(OTHER, "박타인");
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));

        NursingRecord mine = createNursingRecord(11L, me, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 9, 0),
                LocalDateTime.of(2026, 5, 3, 9, 5));
        NursingRecord theirs = createNursingRecord(12L, other, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 8, 0),
                LocalDateTime.of(2026, 5, 3, 8, 5));
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(mine, theirs));
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nursingRecordId()).isEqualTo(11L);
        assertThat(result.get(0).editable()).isTrue();
        assertThat(result.get(1).nursingRecordId()).isEqualTo(12L);
        assertThat(result.get(1).editable()).isFalse();
    }

    @Test
    @DisplayName("draft는 createdAt+editContent, confirmed는 confirmedAt+finalContent로 매핑된다")
    void getNursingNotes_성공_status별_시간_본문_분기() {
        Practitioner nurse = createPractitioner(ME, "이조은");
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));

        LocalDateTime draftCreated = LocalDateTime.of(2026, 5, 3, 9, 0);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 5, 3, 12, 0);

        NursingRecord draft = createNursingRecord(20L, nurse, RecordStatus.draft, draftCreated, null);
        setField(draft, "editContent", "임시 본문");
        setField(draft, "finalContent", null);

        NursingRecord confirmed = createNursingRecord(21L, nurse, RecordStatus.confirmed,
                LocalDateTime.of(2026, 5, 3, 11, 50), confirmedAt);
        setField(confirmed, "editContent", null);
        setField(confirmed, "finalContent", "확정 본문");

        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(draft, confirmed));
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(2);
        NursingNoteItemResponse first = result.get(0);
        assertThat(first.nursingRecordId()).isEqualTo(21L);
        assertThat(first.occurredAt()).isEqualTo(confirmedAt);
        assertThat(first.content()).isEqualTo("확정 본문");

        NursingNoteItemResponse second = result.get(1);
        assertThat(second.nursingRecordId()).isEqualTo(20L);
        assertThat(second.occurredAt()).isEqualTo(draftCreated);
        assertThat(second.content()).isEqualTo("임시 본문");
    }

    @Test
    @DisplayName("medicationOrder가 null이면 frequency/route는 null로 응답된다")
    void getNursingNotes_성공_medicationOrder_null() {
        Practitioner nurse = createPractitioner(ME, "이조은");
        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END)).willReturn(List.of());

        Medication med = createMedication(101L, "PC1", "약A");
        MedicationAdministration ma = createAdmin(31L, nurse, med, null,
                "tag-uuid", LocalDateTime.of(2026, 5, 3, 12, 0),
                RecordStatus.confirmed, true);
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(ma));

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).medications()).hasSize(1);
        assertThat(result.get(0).medications().get(0).frequency()).isNull();
        assertThat(result.get(0).medications().get(0).route()).isNull();
    }

    @Test
    @DisplayName("동일 occurredAt이면 STT_NOTE가 MEDICATION보다 먼저 나온다")
    void getNursingNotes_성공_동일_occurredAt_보조정렬() {
        Practitioner nurse = createPractitioner(ME, "이조은");
        LocalDateTime sameTime = LocalDateTime.of(2026, 5, 3, 14, 0);

        given(encounterRepository.findById(ENCOUNTER_ID))
                .willReturn(Optional.of(createEncounter(ENCOUNTER_ID, WARD_ID)));

        NursingRecord note = createNursingRecord(11L, nurse, RecordStatus.confirmed, sameTime.minusMinutes(5), sameTime);
        given(nursingRecordRepository.findAllByEncounterIdAndDateWithAuthor(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(note));

        Medication med = createMedication(101L, "PC1", "약A");
        MedicationAdministration ma = createAdmin(31L, nurse, med, null,
                "tag-uuid", sameTime, RecordStatus.confirmed, true);
        given(medicationAdministrationRepository.findAllByEncounterIdAndDateWithFetch(ENCOUNTER_ID, DAY_START, DAY_END))
                .willReturn(List.of(ma));

        List<NursingNoteItemResponse> result = nursingNoteService.getNursingNotes(ENCOUNTER_ID, DATE, ME, WARD_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(NursingNoteItemType.STT_NOTE);
        assertThat(result.get(1).type()).isEqualTo(NursingNoteItemType.MEDICATION);
    }

    // --- 헬퍼 ---

    private Encounter createEncounter(Long id, Long wardId) {
        Ward ward = newInstance(Ward.class);
        setField(ward, "wardId", wardId);
        Room room = newInstance(Room.class);
        setField(room, "ward", ward);
        Encounter e = newInstance(Encounter.class);
        setField(e, "encounterId", id);
        setField(e, "room", room);
        return e;
    }

    private Practitioner createPractitioner(Long id, String name) {
        Practitioner p = newInstance(Practitioner.class);
        setField(p, "practitionerId", id);
        setField(p, "name", name);
        return p;
    }

    private Medication createMedication(Long id, String productCode, String productName) {
        Medication m = newInstance(Medication.class);
        setField(m, "medicationId", id);
        setField(m, "productCode", productCode);
        setField(m, "productName", productName);
        return m;
    }

    private NursingRecord createNursingRecord(Long id, Practitioner author, RecordStatus status,
                                              LocalDateTime createdAt, LocalDateTime confirmedAt) {
        NursingRecord nr = newInstance(NursingRecord.class);
        setField(nr, "nursingRecordId", id);
        setField(nr, "authorPractitionerId", author.getPractitionerId());
        setField(nr, "status", status);
        setField(nr, "createdAt", createdAt);
        setField(nr, "confirmedAt", confirmedAt);
        setField(nr, "editContent", "edit-" + id);
        setField(nr, "finalContent", "final-" + id);
        return nr;
    }

    private MedicationAdministration createAdmin(Long id, Practitioner practitioner, Medication medication,
                                                 MedicationOrder order, String taggingId,
                                                 LocalDateTime effectiveDatetime,
                                                 RecordStatus status, boolean nfcTagVerified) {
        MedicationAdministration ma = newInstance(MedicationAdministration.class);
        setField(ma, "medicationAdminId", id);
        setField(ma, "practitioner", practitioner);
        setField(ma, "medication", medication);
        setField(ma, "medicationOrder", order);
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
