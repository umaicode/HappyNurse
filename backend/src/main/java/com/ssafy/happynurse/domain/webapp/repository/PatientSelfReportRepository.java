package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientSelfReportRepository extends JpaRepository<PatientSelfReport, Long> {
}
