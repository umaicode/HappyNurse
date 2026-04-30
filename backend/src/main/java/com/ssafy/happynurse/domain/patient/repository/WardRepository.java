package com.ssafy.happynurse.domain.patient.repository;

import com.ssafy.happynurse.domain.patient.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {

    Optional<Ward> findByWardIdAndOrganization_OrganizationId(Long wardId, Long organizationId);

    List<Ward> findAllByOrganization_OrganizationIdOrderByWardNameAscWardIdAsc(Long organizationId);
}
