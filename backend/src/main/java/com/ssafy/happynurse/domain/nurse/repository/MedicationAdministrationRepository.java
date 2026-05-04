package com.ssafy.happynurse.domain.nurse.repository;

import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {
}
