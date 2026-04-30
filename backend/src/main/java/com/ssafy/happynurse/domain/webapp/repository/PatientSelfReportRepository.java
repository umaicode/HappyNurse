package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientSelfReportRepository extends JpaRepository<PatientSelfReport, Long> {

    @Query("SELECT r FROM PatientSelfReport r "
            + "LEFT JOIN FETCH r.quickSymptomButton "
            + "WHERE r.patient.patientId = :patientId "
            + "AND r.submittedAt >= :dayStart AND r.submittedAt < :dayEnd "
            + "ORDER BY r.submittedAt DESC")
    List<PatientSelfReport> findByPatientIdAndDate(Long patientId, LocalDateTime dayStart, LocalDateTime dayEnd);
}
