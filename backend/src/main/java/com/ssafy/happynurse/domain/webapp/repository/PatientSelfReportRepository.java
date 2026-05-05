package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientSelfReportRepository extends JpaRepository<PatientSelfReport, Long> {

    @Query("SELECT r FROM PatientSelfReport r "
            + "LEFT JOIN FETCH r.quickSymptomButton "
            + "WHERE r.patient.patientId = :patientId "
            + "ORDER BY r.submittedAt DESC")
    List<PatientSelfReport> findByPatientId(@Param("patientId") Long patientId);
}
