package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.ClassCode;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Gender;
import com.ssafy.happynurse.domain.patient.entity.Organization;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EncounterRepository.findCurrentWardIdByPatientId 통합 테스트.
 * 실제 시드 데이터 구조(행복너스병원 → 3병동 → 302호 → 이승연 입원중)와 유사한 형태로 구성.
 */
@DataJpaTest
class EncounterRepositoryFindCurrentWardIdTest {

    @Autowired EntityManager em;
    @Autowired EncounterRepository encounterRepository;

    @Test
    void findCurrentWardIdByPatientId_returnsWardIdForInProgressEncounter() throws Exception {
        // 행복너스병원 → 3병동 → 302호 → 이승연 입원중
        Organization hospital = newOrganization("행복너스병원");
        em.persist(hospital);
        Ward ward = newWard(hospital, "3병동");
        em.persist(ward);
        Room room = newRoom(ward, "302호");
        em.persist(room);
        Patient patient = newPatient(room, "이승연", "MRN-00002",
                Gender.female, LocalDate.of(1999, 7, 25));
        em.persist(patient);
        Encounter enc = newInProgressEncounter(patient, room, "이승연",
                Gender.female, LocalDate.of(1999, 7, 25), "2");
        em.persist(enc);
        em.flush();

        Optional<Long> wardId = encounterRepository.findCurrentWardIdByPatientId(patient.getPatientId());

        assertThat(wardId).contains(ward.getWardId());
    }

    @Test
    void findCurrentWardIdByPatientId_ignoresFinishedEncounter() throws Exception {
        // 박승찬: 과거 입원 finished 상태 → wardId 조회 결과 없음
        Organization hospital = newOrganization("행복너스병원");
        em.persist(hospital);
        Ward ward = newWard(hospital, "3병동");
        em.persist(ward);
        Room room = newRoom(ward, "303호");
        em.persist(room);
        Patient patient = newPatient(room, "박승찬", "MRN-00003",
                Gender.male, LocalDate.of(1999, 11, 3));
        em.persist(patient);
        Encounter enc = newInProgressEncounter(patient, room, "박승찬",
                Gender.male, LocalDate.of(1999, 11, 3), "1");
        setField(enc, "status", EncounterStatus.finished);   // 퇴원 처리
        em.persist(enc);
        em.flush();

        Optional<Long> wardId = encounterRepository.findCurrentWardIdByPatientId(patient.getPatientId());

        assertThat(wardId).isEmpty();
    }

    @Test
    void findCurrentWardIdByPatientId_returnsEmptyForUnknownPatient() {
        Optional<Long> wardId = encounterRepository.findCurrentWardIdByPatientId(999_999L);
        assertThat(wardId).isEmpty();
    }

    // ===========================================================================
    // reflection helpers — 엔티티 생성자/필드가 protected라 테스트에서 직접 주입 (테스트 한정 우회)
    // ===========================================================================

    private Organization newOrganization(String name) throws Exception {
        Organization o = instantiate(Organization.class);
        setField(o, "name", name);
        setField(o, "identifierValue", "ORG-" + name);
        setField(o, "typeCode", "hospital");
        setField(o, "active", true);
        return o;
    }

    private Ward newWard(Organization org, String wardName) throws Exception {
        Ward w = instantiate(Ward.class);
        setField(w, "organization", org);
        setField(w, "wardName", wardName);
        return w;
    }

    private Room newRoom(Ward ward, String roomName) throws Exception {
        Room r = instantiate(Room.class);
        setField(r, "ward", ward);
        setField(r, "roomName", roomName);
        return r;
    }

    private Patient newPatient(Room room, String name, String mrn,
                               Gender gender, LocalDate birthDate) throws Exception {
        Patient p = instantiate(Patient.class);
        setField(p, "currentRoom", room);
        setField(p, "identifierValue", mrn);
        setField(p, "name", name);
        setField(p, "gender", gender);
        setField(p, "birthDate", birthDate);
        setField(p, "active", true);
        return p;
    }

    private Encounter newInProgressEncounter(Patient patient, Room room, String name,
                                             Gender gender, LocalDate birthDate,
                                             String bedName) throws Exception {
        Encounter e = instantiate(Encounter.class);
        setField(e, "patient", patient);
        setField(e, "status", EncounterStatus.in_progress);
        setField(e, "classCode", ClassCode.IMP);
        setField(e, "periodStart", LocalDateTime.of(2026, 5, 1, 10, 0));
        setField(e, "name", name);
        setField(e, "gender", gender);
        setField(e, "birthDate", birthDate);
        setField(e, "room", room);
        setField(e, "bedName", bedName);
        return e;
    }

    /** protected 생성자 우회용 — Constructor에도 setAccessible 필요 */
    private static <T> T instantiate(Class<T> clazz) throws Exception {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try { return c.getDeclaredField(fieldName); }
            catch (NoSuchFieldException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(fieldName);
    }
}