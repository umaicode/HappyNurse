package com.ssafy.happynurse.domain.common.repository;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PractitionerRoleRepository extends JpaRepository<PractitionerRole, Long> {
    List<PractitionerRole> findByPractitionerAndPeriodEndIsNull(Practitioner practitioner);

    Optional<PractitionerRole> findByPractitionerAndWard_WardIdAndPeriodEndIsNull(
            Practitioner practitioner, Long wardId);

    @Query("""                                                                                                                                          
      SELECT pr FROM PractitionerRole pr                                                                                                              
      WHERE pr.practitioner.practitionerId = :practitionerId                           
        AND pr.ward.wardId = :wardId                                                                                                                  
        AND (pr.periodEnd IS NULL OR pr.periodEnd >= CURRENT_DATE)                     
  """)
    Optional<PractitionerRole> findActiveByPractitionerIdAndWardId(
            @Param("practitionerId") Long practitionerId,
            @Param("wardId") Long wardId
    );
}
