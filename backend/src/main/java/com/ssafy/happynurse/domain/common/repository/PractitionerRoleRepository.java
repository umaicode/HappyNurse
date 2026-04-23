package com.ssafy.happynurse.domain.common.repository;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PractitionerRoleRepository extends JpaRepository<PractitionerRole, Long> {
    List<PractitionerRole> findByPractitionerAndPeriodEndIsNull(Practitioner practitioner);
}
