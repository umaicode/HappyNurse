package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    Optional<Encounter> findByPatientAndStatus(Patient patient, EncounterStatus status);
}
