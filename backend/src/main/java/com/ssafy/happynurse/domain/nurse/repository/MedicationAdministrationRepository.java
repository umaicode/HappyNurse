package com.ssafy.happynurse.domain.nurse.repository;

import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("""
            SELECT ma FROM MedicationAdministration ma
            JOIN FETCH ma.practitioner
            JOIN FETCH ma.medication
            LEFT JOIN FETCH ma.medicationOrder
            WHERE ma.taggingId = :taggingId
            ORDER BY ma.medicationAdminId ASC
            """)
    List<MedicationAdministration> findAllByTaggingId(@Param("taggingId") String taggingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicationAdministration ma
            SET ma.status = com.ssafy.happynurse.domain.nurse.entity.RecordStatus.confirmed
            WHERE ma.taggingId = :taggingId
            """)
    int confirmByTaggingId(@Param("taggingId") String taggingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicationAdministration ma
            SET ma.effectiveDatetime = :effectiveDatetime
            WHERE ma.taggingId = :taggingId
            """)
    int updateEffectiveDatetimeByTaggingId(
            @Param("taggingId") String taggingId,
            @Param("effectiveDatetime") LocalDateTime effectiveDatetime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicationAdministration ma
            SET ma.dosageQuantity = :dosageQuantity, ma.dosageUnit = :dosageUnit
            WHERE ma.medicationAdminId = :medicationAdminId
            """)
    int updateDosage(@Param("medicationAdminId") Long medicationAdminId,
                     @Param("dosageQuantity") BigDecimal dosageQuantity,
                     @Param("dosageUnit") String dosageUnit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MedicationAdministration ma WHERE ma.taggingId = :taggingId")
    int deleteByTaggingId(@Param("taggingId") String taggingId);
}
