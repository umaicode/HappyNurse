package com.ssafy.happynurse.domain.watch.repository;

import com.ssafy.happynurse.domain.watch.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
}
