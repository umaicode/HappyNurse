package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.nurse.repository.EncounterDraftCount;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.cache.AssignedPatientsCache;
import com.ssafy.happynurse.domain.patient.dto.WardPatientListResponse;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Gender;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WardPatientListServiceTest {

    @Mock EncounterRepository encounterRepository;
    @Mock NursingRecordRepository nursingRecordRepository;
    @Mock AssignedPatientsCache assignedPatientsCache;
    @InjectMocks WardPatientListService wardPatientListService;

    @Test
    @DisplayName("listWardPatients - Redis hit 시 캐시 멤버십 기준으로 isMyPatient 가 매핑되고 DB 폴백 쿼리는 호출되지 않는다")
    void listWardPatients_캐시_히트_isMyPatient_매핑() {
        Long wardId = 3L;
        Long me = 99L;
        Encounter e1 = createEncounter(100L, "김가민", Gender.female,
                LocalDate.of(1999, 5, 20), "7101호", "A");
        Encounter e2 = createEncounter(101L, "이영희", Gender.female,
                LocalDate.of(1985, 11, 3), "7102호", "B");
        given(encounterRepository.findInProgressByWard(wardId)).willReturn(List.of(e1, e2));
        given(nursingRecordRepository.countDraftByEncounterIds(anyList()))
                .willReturn(List.of());
        given(assignedPatientsCache.read(me, wardId)).willReturn(Optional.of(Set.of(100L)));

        List<WardPatientListResponse> result = wardPatientListService.listWardPatients(wardId, me);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).encounterId()).isEqualTo(100L);
        assertThat(result.get(0).isMyPatient()).isTrue();
        assertThat(result.get(1).encounterId()).isEqualTo(101L);
        assertThat(result.get(1).isMyPatient()).isFalse();
        verify(encounterRepository, never())
                .findInProgressByWardAndAssignedPractitioner(anyLong(), anyLong());
    }

    @Test
    @DisplayName("listWardPatients - 빈 결과면 카운트/캐시 조회 없이 빈 리스트")
    void listWardPatients_빈_결과() {
        given(encounterRepository.findInProgressByWard(3L)).willReturn(List.of());

        List<WardPatientListResponse> result = wardPatientListService.listWardPatients(3L, 99L);

        assertThat(result).isEmpty();
        verify(nursingRecordRepository, never()).countDraftByEncounterIds(any());
        verify(assignedPatientsCache, never()).read(anyLong(), anyLong());
    }

    @Test
    @DisplayName("listWardPatients - Redis miss 시 DB 폴백으로 isMyPatient 계산하고 결과를 Redis 에 warm-up")
    void listWardPatients_캐시_미스_DB_폴백_warmup() {
        Long wardId = 3L;
        Long me = 99L;
        Encounter e1 = createEncounter(100L, "김가민", Gender.female,
                LocalDate.of(1999, 5, 20), "7101호", "A");
        Encounter e2 = createEncounter(101L, "이영희", Gender.female,
                LocalDate.of(1985, 11, 3), "7102호", "B");
        Encounter mineFromDb = createEncounter(101L, "이영희", Gender.female,
                LocalDate.of(1985, 11, 3), "7102호", "B");
        given(encounterRepository.findInProgressByWard(wardId)).willReturn(List.of(e1, e2));
        given(nursingRecordRepository.countDraftByEncounterIds(anyList()))
                .willReturn(List.of());
        given(assignedPatientsCache.read(me, wardId)).willReturn(Optional.empty());
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of(mineFromDb));

        List<WardPatientListResponse> result = wardPatientListService.listWardPatients(wardId, me);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isMyPatient()).isFalse();
        assertThat(result.get(1).isMyPatient()).isTrue();
        verify(assignedPatientsCache).write(eq(me), eq(wardId), eq(Set.of(101L)));
    }

    @Test
    @DisplayName("listWardPatients - Redis miss + DB 폴백도 빈 결과면 모든 환자 isMyPatient false")
    void listWardPatients_캐시_미스_DB도_비어있으면_모두_false() {
        Long wardId = 3L;
        Long me = 99L;
        Encounter e1 = createEncounter(100L, "김가민", Gender.female,
                LocalDate.of(1999, 5, 20), "7101호", "A");
        given(encounterRepository.findInProgressByWard(wardId)).willReturn(List.of(e1));
        given(nursingRecordRepository.countDraftByEncounterIds(anyList()))
                .willReturn(List.of());
        given(assignedPatientsCache.read(me, wardId)).willReturn(Optional.empty());
        given(encounterRepository.findInProgressByWardAndAssignedPractitioner(wardId, me))
                .willReturn(List.of());

        List<WardPatientListResponse> result = wardPatientListService.listWardPatients(wardId, me);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isMyPatient()).isFalse();
        verify(assignedPatientsCache).write(eq(me), eq(wardId), eq(Set.of()));
    }

    @Test
    @DisplayName("listWardPatients - 미확정 일지 카운트가 환자별로 정확히 매핑된다")
    void listWardPatients_미확정일지_카운트_매핑() {
        Long wardId = 3L;
        Long me = 99L;
        Encounter e1 = createEncounter(100L, "김가민", Gender.female,
                LocalDate.of(1999, 5, 20), "7101호", "A");
        Encounter e2 = createEncounter(101L, "이영희", Gender.female,
                LocalDate.of(1985, 11, 3), "7102호", "B");
        Encounter e3 = createEncounter(102L, "박철수", Gender.male,
                LocalDate.of(1970, 1, 1), "7103호", "C");
        EncounterDraftCount c100 = mockDraftCount(100L, 2L);
        EncounterDraftCount c102 = mockDraftCount(102L, 5L);
        given(encounterRepository.findInProgressByWard(wardId)).willReturn(List.of(e1, e2, e3));
        given(nursingRecordRepository.countDraftByEncounterIds(anyList()))
                .willReturn(List.of(c100, c102));
        given(assignedPatientsCache.read(me, wardId)).willReturn(Optional.of(Set.of()));

        List<WardPatientListResponse> result = wardPatientListService.listWardPatients(wardId, me);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).unconfirmedNursingCount()).isEqualTo(2L);
        assertThat(result.get(1).unconfirmedNursingCount()).isZero();
        assertThat(result.get(2).unconfirmedNursingCount()).isEqualTo(5L);
    }

    // ──── 헬퍼 ────

    private EncounterDraftCount mockDraftCount(Long encounterId, Long cnt) {
        EncounterDraftCount row = mock(EncounterDraftCount.class);
        given(row.getEncounterId()).willReturn(encounterId);
        given(row.getCnt()).willReturn(cnt);
        return row;
    }

    private Encounter createEncounter(Long encounterId, String name, Gender gender,
                                      LocalDate birthDate, String roomName, String bedName) {
        try {
            Room room = createRoom(encounterId * 100, roomName);

            var ctor = Encounter.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Encounter e = ctor.newInstance();
            setField(e, "encounterId", encounterId);
            setField(e, "room", room);
            setField(e, "name", name);
            setField(e, "gender", gender);
            setField(e, "birthDate", birthDate);
            setField(e, "bedName", bedName);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Room createRoom(Long id, String roomName) {
        try {
            var ctor = Room.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Room r = ctor.newInstance();
            setField(r, "roomId", id);
            setField(r, "roomName", roomName);
            return r;
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
