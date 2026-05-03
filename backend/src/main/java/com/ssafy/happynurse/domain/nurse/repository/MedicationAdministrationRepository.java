package com.ssafy.happynurse.domain.nurse.repository;

import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationAdministrationRepository
        extends JpaRepository<MedicationAdministration, Long> {

    @Query("""
            SELECT ma FROM MedicationAdministration ma
            JOIN FETCH ma.practitioner
            JOIN FETCH ma.medication
            LEFT JOIN FETCH ma.medicationOrder
            WHERE ma.encounter.encounterId = :encounterId
              AND ma.effectiveDatetime >= :dayStart
              AND ma.effectiveDatetime < :dayEnd
            ORDER BY ma.taggingId ASC, ma.medicationAdminId ASC
            """)
    List<MedicationAdministration> findAllByEncounterIdAndDateWithFetch(
            @Param("encounterId") Long encounterId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
