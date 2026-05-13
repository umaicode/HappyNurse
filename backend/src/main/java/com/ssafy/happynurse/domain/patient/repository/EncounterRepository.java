package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    Optional<Encounter> findByPatientAndStatus(Patient patient, EncounterStatus status);

    /**
     * 환자 상세 페이지용 — encounter + room + ward + attendingPhysician 을 한 쿼리로 LEFT JOIN FETCH
     */
    @EntityGraph(attributePaths = {"room", "room.ward", "attendingPhysician"})
    Optional<Encounter> findWithDetailsByPatientAndStatus(Patient patient, EncounterStatus status);

    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.room r
            LEFT JOIN FETCH e.assignedPractitioner
            WHERE r.ward.wardId = :wardId
              AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress
            ORDER BY r.roomName ASC, e.bedName ASC, e.encounterId ASC
            """)
    List<Encounter> findInProgressByWard(@Param("wardId") Long wardId);

    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.room r
            JOIN e.assignedPractitioner ap
            WHERE r.ward.wardId = :wardId
              AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress
              AND ap.practitionerId = :assignedPractitionerId
            ORDER BY r.roomName ASC, e.bedName ASC, e.encounterId ASC
            """)
    List<Encounter> findInProgressByWardAndAssignedPractitioner(
            @Param("wardId") Long wardId,
            @Param("assignedPractitionerId") Long assignedPractitionerId);

    @Query("""
            SELECT e FROM Encounter e
            LEFT JOIN FETCH e.assignedPractitioner
            JOIN e.room r
            WHERE e.encounterId IN :encounterIds
              AND r.ward.wardId = :wardId
              AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress
            """)
    List<Encounter> findAllByIdInAndWardAndInProgress(
            @Param("encounterIds") Collection<Long> encounterIds,
            @Param("wardId") Long wardId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Encounter e SET e.assignedPractitioner = :nurse WHERE e.encounterId IN :encounterIds")
    int assignNurseToEncounters(
            @Param("nurse") Practitioner nurse,
            @Param("encounterIds") Collection<Long> encounterIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Encounter e SET e.assignedPractitioner = null WHERE e.encounterId IN :encounterIds AND e.assignedPractitioner.practitionerId = :nurseId")
    int unassignNurseWhereStillOwned(
            @Param("encounterIds") Collection<Long> encounterIds,
            @Param("nurseId") Long nurseId);

    /**
     * 환자의 현재(in_progress) 입원이 속한 ward_id 를 단일 쿼리로 조회.
     * 알림 라우팅 시 envelope.wardId 채우는 용도.
     *
     * 도메인 규약(troubleshooting 2026-04-29): 입원 중 환자 정보는 encounter 가 진실원.
     * 환자가 전실되어도 그 시점의 입원 컨텍스트가 wardId 의 진실원이다.
     */
    @Query("""                                                                                                                                          
          SELECT e.room.ward.wardId                                                                                                                   
          FROM Encounter e                                     
          WHERE e.patient.patientId = :patientId                                                                                                      
            AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress                                                     
          """)
    Optional<Long> findCurrentWardIdByPatientId(@Param("patientId") Long patientId);

    /**
     * HIS 발사기 — in_progress 입원에 담당 간호사로 배정된 Practitioner DISTINCT 목록.
     * ORDER BY는 PostgreSQL DISTINCT 제약으로 제외 — 호출측에서 Java 정렬.
     */
    @Query("""
            SELECT DISTINCT e.assignedPractitioner FROM Encounter e
            WHERE e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress
              AND e.assignedPractitioner IS NOT NULL
            """)
    List<Practitioner> findDistinctAssignedPractitionersByInProgress();

    /**
     * HIS 발사기 — 특정 간호사가 담당인 in_progress 입원 목록.
     */
    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.patient p
            JOIN FETCH e.room r
            WHERE e.assignedPractitioner.practitionerId = :nurseId
              AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress
            ORDER BY r.roomName ASC, e.bedName ASC
            """)
    List<Encounter> findInProgressByAssignedPractitioner(@Param("nurseId") Long nurseId);

    /**
     * 인수인계 — 특정 병동에서 [start, end) 사이에 입원한 (periodStart 기준) encounter 목록.
     */
    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.room r
            WHERE r.ward.wardId = :wardId
              AND e.periodStart >= :startInclusive
              AND e.periodStart < :endExclusive
            ORDER BY e.periodStart ASC
            """)
    List<Encounter> findAdmissionsByWardAndPeriod(
            @Param("wardId") Long wardId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 인수인계 — 특정 병동에서 [start, end) 사이에 퇴원한 (periodEnd 기준, status=finished) encounter 목록.
     */
    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.room r
            WHERE r.ward.wardId = :wardId
              AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.finished
              AND e.periodEnd >= :startInclusive
              AND e.periodEnd < :endExclusive
            ORDER BY e.periodEnd ASC
            """)
    List<Encounter> findDischargesByWardAndPeriod(
            @Param("wardId") Long wardId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive);

}
