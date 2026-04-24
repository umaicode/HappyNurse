package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
