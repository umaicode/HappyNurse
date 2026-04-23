package com.ssafy.happynurse.domain.common.repository;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PractitionerRepository extends JpaRepository<Practitioner, Long> {
    Optional<Practitioner> findByEmployeeNumber(String employeeNumber);
}
