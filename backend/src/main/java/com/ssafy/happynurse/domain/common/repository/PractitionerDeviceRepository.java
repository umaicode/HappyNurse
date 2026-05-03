package com.ssafy.happynurse.domain.common.repository;

import com.ssafy.happynurse.domain.common.entity.PractitionerDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PractitionerDeviceRepository extends JpaRepository<PractitionerDevice, Long> {

    Optional<PractitionerDevice> findByPractitionerRole_PractitionerRoleIdAndFcmToken(
            Long practitionerRoleId, String fcmToken);

    @Query("""                                                                                                                                      
          SELECT d FROM PractitionerDevice d                                           
          WHERE d.practitionerRole.practitioner.practitionerId = :practitionerId                                                                      
            AND d.isActive = true                                                      
      """)
    List<PractitionerDevice> findActiveByPractitionerId(@Param("practitionerId") Long practitionerId);
}