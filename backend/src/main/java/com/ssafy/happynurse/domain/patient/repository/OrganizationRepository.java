package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findAllByActiveTrueOrderByNameAscOrganizationIdAsc();
}
