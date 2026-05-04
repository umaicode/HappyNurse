package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordManualCreateRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordUpdateRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordWriteResponse;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecordFactory;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NursingRecordServiceTest {

    @Mock
    NursingRecordRepository nursingRecordRepository;
    @Mock
    EncounterRepository encounterRepository;
    @Mock
    PractitionerRepository practitionerRepository;
    @Spy
    NursingRecordFactory nursingRecordFactory = new NursingRecordFactory();
    @InjectMocks
    NursingRecordService nursingRecordService;

    private static final Long ID = 12L;
    private static final Long ME = 6L;
    private static final Long OTHER = 99L;
    private static final Long ENCOUNTER_ID = 42L;
    private static final Long PATIENT_ID = 7L;

    @Test
    @DisplayName("수동 작성 → 바로 confirmed 상태로 저장, STT 필드는 null")
    void createManual_성공() {
        Patient patient = createPatient(PATIENT_ID);
        Encounter encounter = createEncounter(ENCOUNTER_ID, patient);
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(practitionerRepository.existsById(ME)).willReturn(true);
        given(nursingRecordRepository.save(any(NursingRecord.class))).willAnswer(inv -> {
            NursingRecord arg = inv.getArgument(0);
            setField(arg, "nursingRecordId", ID);
            return arg;
        });

        LocalDateTime before = LocalDateTime.now();
        NursingRecordWriteResponse response = nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(ENCOUNTER_ID, "수동 입력 본문"), ME);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<NursingRecord> captor = ArgumentCaptor.forClass(NursingRecord.class);
        verify(nursingRecordRepository).save(captor.capture());
        NursingRecord saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(RecordStatus.confirmed);
        assertThat(saved.getFinalContent()).isEqualTo("수동 입력 본문");
        assertThat(saved.getEditContent()).isNull();
        assertThat(saved.getAudioFileUrl()).isNull();
        assertThat(saved.getOriginalSttContent()).isNull();
        assertThat(saved.getEditorStateJson()).isNull();
        assertThat(saved.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(saved.getEncounterId()).isEqualTo(ENCOUNTER_ID);
        assertThat(saved.getAuthorPractitionerId()).isEqualTo(ME);
        assertThat(saved.getConfirmedAt()).isBetween(before, after);

        assertThat(response.nursingRecordId()).isEqualTo(ID);
        assertThat(response.status()).isEqualTo(RecordStatus.confirmed);
        assertThat(response.content()).isEqualTo("수동 입력 본문");
        assertThat(response.confirmedAt()).isEqualTo(saved.getConfirmedAt());
    }

    @Test
    @DisplayName("수동 작성 시 encounterId null → INVALID_INPUT_VALUE")
    void createManual_실패_encounterId_null() {
        assertThatThrownBy(() -> nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(null, "본문"), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(nursingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("수동 작성 시 본문 null → INVALID_INPUT_VALUE")
    void createManual_실패_본문_null() {
        assertThatThrownBy(() -> nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(ENCOUNTER_ID, null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(nursingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("수동 작성 시 본문이 공백뿐 → INVALID_INPUT_VALUE")
    void createManual_실패_본문_blank() {
        assertThatThrownBy(() -> nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(ENCOUNTER_ID, "   "), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(nursingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("수동 작성 시 입원 정보 없음 → ENCOUNTER_NOT_FOUND")
    void createManual_실패_encounter_없음() {
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(ENCOUNTER_ID, "본문"), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENCOUNTER_NOT_FOUND);
        verify(nursingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("수동 작성 시 의료진 정보 없음 → PRACTITIONER_NOT_FOUND")
    void createManual_실패_practitioner_없음() {
        Patient patient = createPatient(PATIENT_ID);
        Encounter encounter = createEncounter(ENCOUNTER_ID, patient);
        given(encounterRepository.findById(ENCOUNTER_ID)).willReturn(Optional.of(encounter));
        given(practitionerRepository.existsById(ME)).willReturn(false);

        assertThatThrownBy(() -> nursingRecordService.createManual(
                new NursingRecordManualCreateRequest(ENCOUNTER_ID, "본문"), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRACTITIONER_NOT_FOUND);
        verify(nursingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("draft 확정 시 finalContent=editContent, confirmedAt=createdAt 복사")
    void confirm_성공() {
        Practitioner author = createPractitioner(ME);
        LocalDateTime created = LocalDateTime.of(2026, 5, 3, 14, 30);
        NursingRecord record = createRecord(ID, ME, RecordStatus.draft, created, null,
                "녹음 본문", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        NursingRecordWriteResponse response = nursingRecordService.confirm(ID, ME);

        assertThat(response.nursingRecordId()).isEqualTo(ID);
        assertThat(response.status()).isEqualTo(RecordStatus.confirmed);
        assertThat(response.content()).isEqualTo("녹음 본문");
        assertThat(response.confirmedAt()).isEqualTo(created);
        verify(nursingRecordRepository).confirmDraft(ID, "녹음 본문", created);
    }

    @Test
    @DisplayName("간호 기록 없음 → NURSING_RECORD_NOT_FOUND")
    void confirm_실패_없음() {
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nursingRecordService.confirm(ID, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("타인이 작성한 기록 → NURSING_RECORD_NOT_AUTHOR")
    void confirm_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        NursingRecord record = createRecord(ID, OTHER, RecordStatus.draft,
                LocalDateTime.now(), null, "본문", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> nursingRecordService.confirm(ID, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_AUTHOR);
    }

    @Test
    @DisplayName("draft가 아닌 상태 확정 시도 → INVALID_RECORD_STATUS")
    void confirm_실패_draft_아님() {
        Practitioner author = createPractitioner(ME);
        NursingRecord record = createRecord(ID, ME, RecordStatus.confirmed,
                LocalDateTime.now(), LocalDateTime.now(), null, "확정 본문");
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> nursingRecordService.confirm(ID, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RECORD_STATUS);
    }

    @Test
    @DisplayName("draft 상태에서 본문 수정 → editContent 갱신, status 유지")
    void update_성공_draft_본문() {
        Practitioner author = createPractitioner(ME);
        NursingRecord before = createRecord(ID, ME, RecordStatus.draft,
                LocalDateTime.now(), null, "원본", null);
        NursingRecord after = createRecord(ID, ME, RecordStatus.draft,
                before.getCreatedAt(), null, "수정본", null);
        given(nursingRecordRepository.findById(ID))
                .willReturn(Optional.of(before))
                .willReturn(Optional.of(after));

        NursingRecordWriteResponse response = nursingRecordService.update(
                ID, new NursingRecordUpdateRequest("수정본", null), ME);

        verify(nursingRecordRepository).updateDraftContent(ID, "수정본");
        verify(nursingRecordRepository, never()).updateContentAsAmended(anyLong(), any());
        assertThat(response.status()).isEqualTo(RecordStatus.draft);
        assertThat(response.content()).isEqualTo("수정본");
    }

    @Test
    @DisplayName("confirmed 상태에서 본문 수정 → finalContent 갱신 + status=amended")
    void update_성공_confirmed_본문() {
        Practitioner author = createPractitioner(ME);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 5, 3, 14, 0);
        NursingRecord before = createRecord(ID, ME, RecordStatus.confirmed,
                LocalDateTime.now(), confirmedAt, null, "기존 확정본");
        NursingRecord after = createRecord(ID, ME, RecordStatus.amended,
                before.getCreatedAt(), confirmedAt, null, "수정본");
        given(nursingRecordRepository.findById(ID))
                .willReturn(Optional.of(before))
                .willReturn(Optional.of(after));

        NursingRecordWriteResponse response = nursingRecordService.update(
                ID, new NursingRecordUpdateRequest("수정본", null), ME);

        verify(nursingRecordRepository).updateContentAsAmended(ID, "수정본");
        verify(nursingRecordRepository, never()).updateDraftContent(anyLong(), any());
        assertThat(response.status()).isEqualTo(RecordStatus.amended);
        assertThat(response.content()).isEqualTo("수정본");
    }

    @Test
    @DisplayName("confirmedAt만 수정 → updateConfirmedAt 호출, 본문 갱신 X")
    void update_성공_confirmedAt만() {
        Practitioner author = createPractitioner(ME);
        LocalDateTime newAt = LocalDateTime.of(2026, 5, 3, 12, 0);
        NursingRecord before = createRecord(ID, ME, RecordStatus.confirmed,
                LocalDateTime.now(), LocalDateTime.now(), null, "본문");
        NursingRecord after = createRecord(ID, ME, RecordStatus.confirmed,
                before.getCreatedAt(), newAt, null, "본문");
        given(nursingRecordRepository.findById(ID))
                .willReturn(Optional.of(before))
                .willReturn(Optional.of(after));

        NursingRecordWriteResponse response = nursingRecordService.update(
                ID, new NursingRecordUpdateRequest(null, newAt), ME);

        verify(nursingRecordRepository).updateConfirmedAt(ID, newAt);
        verify(nursingRecordRepository, never()).updateDraftContent(anyLong(), any());
        verify(nursingRecordRepository, never()).updateContentAsAmended(anyLong(), any());
        assertThat(response.confirmedAt()).isEqualTo(newAt);
    }

    @Test
    @DisplayName("content + confirmedAt 둘 다 수정")
    void update_성공_둘다() {
        Practitioner author = createPractitioner(ME);
        LocalDateTime newAt = LocalDateTime.of(2026, 5, 3, 12, 0);
        NursingRecord before = createRecord(ID, ME, RecordStatus.confirmed,
                LocalDateTime.now(), LocalDateTime.now(), null, "기존");
        NursingRecord after = createRecord(ID, ME, RecordStatus.amended,
                before.getCreatedAt(), newAt, null, "수정본");
        given(nursingRecordRepository.findById(ID))
                .willReturn(Optional.of(before))
                .willReturn(Optional.of(after));

        nursingRecordService.update(ID, new NursingRecordUpdateRequest("수정본", newAt), ME);

        verify(nursingRecordRepository).updateContentAsAmended(ID, "수정본");
        verify(nursingRecordRepository).updateConfirmedAt(ID, newAt);
    }

    @Test
    @DisplayName("빈 본문 수정 시도 → INVALID_INPUT_VALUE")
    void update_실패_본문_blank() {
        Practitioner author = createPractitioner(ME);
        NursingRecord record = createRecord(ID, ME, RecordStatus.draft,
                LocalDateTime.now(), null, "원본", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> nursingRecordService.update(
                ID, new NursingRecordUpdateRequest("   ", null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("update 시 기록 없음 → NURSING_RECORD_NOT_FOUND")
    void update_실패_없음() {
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nursingRecordService.update(
                ID, new NursingRecordUpdateRequest("본문", null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("update 시 타작성자 → NURSING_RECORD_NOT_AUTHOR")
    void update_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        NursingRecord record = createRecord(ID, OTHER, RecordStatus.draft,
                LocalDateTime.now(), null, "본문", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> nursingRecordService.update(
                ID, new NursingRecordUpdateRequest("수정본", null), ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_AUTHOR);
    }

    @Test
    @DisplayName("delete 성공")
    void delete_성공() {
        Practitioner author = createPractitioner(ME);
        NursingRecord record = createRecord(ID, ME, RecordStatus.draft,
                LocalDateTime.now(), null, "본문", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        nursingRecordService.delete(ID, ME);

        verify(nursingRecordRepository, times(1)).delete(record);
    }

    @Test
    @DisplayName("delete 시 기록 없음 → NURSING_RECORD_NOT_FOUND")
    void delete_실패_없음() {
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> nursingRecordService.delete(ID, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("delete 시 타작성자 → NURSING_RECORD_NOT_AUTHOR")
    void delete_실패_타작성자() {
        Practitioner other = createPractitioner(OTHER);
        NursingRecord record = createRecord(ID, OTHER, RecordStatus.draft,
                LocalDateTime.now(), null, "본문", null);
        given(nursingRecordRepository.findById(ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> nursingRecordService.delete(ID, ME))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NURSING_RECORD_NOT_AUTHOR);
    }

    // --- 헬퍼 ---

    private Practitioner createPractitioner(Long id) {
        Practitioner p = newInstance(Practitioner.class);
        setField(p, "practitionerId", id);
        setField(p, "name", "이조은");
        return p;
    }

    private Patient createPatient(Long id) {
        Patient p = newInstance(Patient.class);
        setField(p, "patientId", id);
        return p;
    }

    private Encounter createEncounter(Long id, Patient patient) {
        Encounter e = newInstance(Encounter.class);
        setField(e, "encounterId", id);
        setField(e, "patient", patient);
        return e;
    }

    private NursingRecord createRecord(Long id, Long authorId, RecordStatus status,
                                       LocalDateTime createdAt, LocalDateTime confirmedAt,
                                       String editContent, String finalContent) {
        NursingRecord nr = newInstance(NursingRecord.class);
        setField(nr, "nursingRecordId", id);
        setField(nr, "authorPractitionerId", authorId);
        setField(nr, "status", status);
        setField(nr, "createdAt", createdAt);
        setField(nr, "confirmedAt", confirmedAt);
        setField(nr, "editContent", editContent);
        setField(nr, "finalContent", finalContent);
        return nr;
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
