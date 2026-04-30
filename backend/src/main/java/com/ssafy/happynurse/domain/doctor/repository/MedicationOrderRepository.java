package com.ssafy.happynurse.domain.doctor.repository;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, Long> {

    @Query("SELECT mo FROM MedicationOrder mo "
            + "JOIN FETCH mo.patient "
            + "JOIN FETCH mo.prescriber "
            + "WHERE mo.patient.patientId = :patientId "
            + "ORDER BY mo.dateWritten DESC")
    List<MedicationOrder> findByPatientIdWithPrescriber(Long patientId);
}