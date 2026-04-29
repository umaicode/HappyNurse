package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    Optional<Encounter> findByPatientAndStatus(Patient patient, EncounterStatus status);

    @Query("""
            SELECT e FROM Encounter e
            JOIN FETCH e.room r
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
}
