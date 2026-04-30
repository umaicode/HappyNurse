package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.Patient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByNfcToken(String nfcToken);
}