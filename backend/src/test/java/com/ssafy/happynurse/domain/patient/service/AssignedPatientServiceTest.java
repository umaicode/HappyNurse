package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateRequest;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateResponse;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssignedPatientServiceTest {

    @Mock EncounterRepository encounterRepository;
    @Mock PractitionerRepository practitionerRepository;
    @InjectMocks AssignedPatientService assignedPatientService;

    @Test
    @DisplayName("updateMyAssignedPatients - 신규 할당 + 이전 담당 해제 동시 처리")
    void update_신규할당_이전해제_동시처리() {
        Long me = 99L;
        Long wardId = 3L;
        Practitioner nurseRef = mockPractitioner(me);
        // 요청 [100, 101] — 100 은 이전에 다른 사람 담당, 101 은 비어있음
        Encounter e100 = createEncounter(100L, mockPractitioner(50L));
        Encounter e101 = createEncounter(101L, null);
        // DB 상 본인 담당이던 것: [202, 203]
        Encounter dbMine202 = createEncounter(202L, nurseRef);
        Encounter dbMine203 = createEncounter(203L, nurseRef);

        given(encounterRepository.findAllByIdInAndWardAndInProgress(any(), eq(wardId)))
                .willReturn(List.of(e100, e101));
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of(dbMine202, dbMine203));
        given(practitionerRepository.getReferenceById(me)).willReturn(nurseRef);

        AssignedPatientUpdateResponse result = assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of(100L, 101L)));

        assertThat(result.assignedEncounterIds()).containsExactly(100L, 101L);
        assertThat(result.releasedEncounterIds()).containsExactly(202L, 203L);
        assertThat(result.overwroteFromOthersCount()).isEqualTo(1);

        verify(encounterRepository).assignNurseToEncounters(eq(nurseRef), eq(Set.of(100L, 101L)));
        verify(encounterRepository).unassignNurseWhereStillOwned(eq(List.of(202L, 203L)), eq(me));
    }

    @Test
    @DisplayName("updateMyAssignedPatients - 빈 배열이면 DB 할당 호출 없이 본인 이전 담당만 해제")
    void update_빈배열_모두해제() {
        Long me = 99L;
        Long wardId = 3L;
        Practitioner nurseRef = mockPractitioner(me);
        Encounter dbMine301 = createEncounter(301L, nurseRef);

        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of(dbMine301));

        AssignedPatientUpdateResponse result = assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of()));

        assertThat(result.assignedEncounterIds()).isEmpty();
        assertThat(result.releasedEncounterIds()).containsExactly(301L);
        assertThat(result.overwroteFromOthersCount()).isZero();

        verify(encounterRepository, never()).findAllByIdInAndWardAndInProgress(any(), anyLong());
        verify(encounterRepository, never()).assignNurseToEncounters(any(), anyCollection());
        verify(encounterRepository).unassignNurseWhereStillOwned(eq(List.of(301L)), eq(me));
    }

    @Test
    @DisplayName("updateMyAssignedPatients - 다른 병동 또는 비-in_progress encounterId 가 섞여 있으면 403 ENCOUNTER_NOT_IN_MY_WARD")
    void update_다른병동_ID_섞이면_403() {
        Long me = 99L;
        Long wardId = 3L;
        // 요청 [100, 999] 인데 999 는 다른 병동 → 조회 결과는 [e100] 만
        Encounter e100 = createEncounter(100L, null);
        given(encounterRepository.findAllByIdInAndWardAndInProgress(any(), eq(wardId)))
                .willReturn(List.of(e100));

        assertThatThrownBy(() -> assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of(100L, 999L))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);

        verify(encounterRepository, never()).assignNurseToEncounters(any(), anyCollection());
        verify(encounterRepository, never()).unassignNurseWhereStillOwned(anyCollection(), anyLong());
    }

    @Test
    @DisplayName("updateMyAssignedPatients - 다른 사람이 가져간 입원은 해제 대상에서 제외(unassign 호출 안 함)")
    void update_다른사람이_가져간_건은_해제_안함() {
        Long me = 99L;
        Long wardId = 3L;
        Practitioner nurseRef = mockPractitioner(me);
        Encounter e500 = createEncounter(500L, null);
        // DB 상 본인 담당으로 조회되는 것이 없으면 (이미 다 빼앗긴 상태) released 도 비어 있음
        given(encounterRepository.findAllByIdInAndWardAndInProgress(any(), eq(wardId)))
                .willReturn(List.of(e500));
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of());
        given(practitionerRepository.getReferenceById(me)).willReturn(nurseRef);

        AssignedPatientUpdateResponse result = assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of(500L)));

        assertThat(result.assignedEncounterIds()).containsExactly(500L);
        assertThat(result.releasedEncounterIds()).isEmpty();
        verify(encounterRepository, never()).unassignNurseWhereStillOwned(anyCollection(), anyLong());
    }

    @Test
    @DisplayName("updateMyAssignedPatients - assignList 중 본인이 이미 담당이던 건은 overwroteFromOthersCount 에서 제외")
    void update_본인_담당이던_건은_overwrote_카운트_제외() {
        Long me = 99L;
        Long wardId = 3L;
        Practitioner nurseRef = mockPractitioner(me);
        Practitioner other = mockPractitioner(50L);
        // 100 은 본인 담당, 101 은 다른 사람 담당, 102 는 비어있음
        Encounter e100 = createEncounter(100L, nurseRef);
        Encounter e101 = createEncounter(101L, other);
        Encounter e102 = createEncounter(102L, null);

        given(encounterRepository.findAllByIdInAndWardAndInProgress(any(), eq(wardId)))
                .willReturn(List.of(e100, e101, e102));
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of(e100));
        given(practitionerRepository.getReferenceById(me)).willReturn(nurseRef);

        AssignedPatientUpdateResponse result = assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of(100L, 101L, 102L)));

        assertThat(result.overwroteFromOthersCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateMyAssignedPatients - 시프트 교대: B 가 같은 환자를 가져와도 A 의 다음 호출에서 정상 처리")
    void update_시프트_교대_시나리오() {
        Long me = 99L; // Nurse A
        Long wardId = 3L;
        Practitioner nurseRefA = mockPractitioner(me);
        Practitioner nurseB = mockPractitioner(77L);
        // Encounter 100 은 현재 B 담당. A 가 다시 가져오기 시도.
        Encounter e100 = createEncounter(100L, nurseB);

        given(encounterRepository.findAllByIdInAndWardAndInProgress(any(), eq(wardId)))
                .willReturn(List.of(e100));
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of()); // A 의 DB 담당은 이미 비어있음 (B 가 빼앗아감)
        given(practitionerRepository.getReferenceById(me)).willReturn(nurseRefA);

        AssignedPatientUpdateResponse result = assignedPatientService.updateMyAssignedPatients(
                me, wardId, new AssignedPatientUpdateRequest(List.of(100L)));

        assertThat(result.assignedEncounterIds()).containsExactly(100L);
        assertThat(result.releasedEncounterIds()).isEmpty();
        assertThat(result.overwroteFromOthersCount()).isEqualTo(1);

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(encounterRepository).assignNurseToEncounters(eq(nurseRefA), idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(100L);
    }

    // ──── 헬퍼 ────

    private Practitioner mockPractitioner(Long id) {
        try {
            var ctor = Practitioner.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Practitioner p = ctor.newInstance();
            setField(p, "practitionerId", id);
            return p;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Encounter createEncounter(Long encounterId, Practitioner assigned) {
        try {
            var ctor = Encounter.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Encounter e = ctor.newInstance();
            setField(e, "encounterId", encounterId);
            setField(e, "assignedPractitioner", assigned);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
