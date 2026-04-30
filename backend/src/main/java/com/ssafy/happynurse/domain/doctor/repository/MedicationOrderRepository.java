package com.ssafy.happynurse.domain.doctor.repository;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, Long> {

    @Query("SELECT mo FROM MedicationOrder mo "
            + "JOIN FETCH mo.patient "
            + "JOIN FETCH mo.prescriber "
            + "WHERE mo.patient.patientId = :patientId "
            + "AND mo.dateWritten >= :dayStart AND mo.dateWritten < :dayEnd "
            + "ORDER BY mo.dateWritten DESC")
    List<MedicationOrder> findByPatientIdAndDate(
            @Param("patientId") Long patientId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}