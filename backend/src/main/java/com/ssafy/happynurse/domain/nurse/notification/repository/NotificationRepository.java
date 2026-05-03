package com.ssafy.happynurse.domain.nurse.notification.repository;

import com.ssafy.happynurse.domain.nurse.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 병동 알림함 — patient → encounter(in_progress) → room → ward 4단계 조인.
     * since/before nullable 처리는 COALESCE 패턴 사용 (PostgreSQL JDBC 의 42P18
     * indeterminate datatype 회피용 — :param IS NULL OR ... 패턴은 같은 파라미터를
     * 두 컨텍스트로 사용해 PostgreSQL 이 타입 추론 못 함).
     */
    @Query("""                                                                                                                                      
              SELECT n FROM Notification n                       
              JOIN n.patient p                                                                                                                        
              JOIN Encounter e ON e.patient = p
                  AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress                                               
              WHERE e.room.ward.wardId = :wardId                                                                                                      
                AND n.createdAt >= COALESCE(:since, n.createdAt) 
                AND n.notificationId < COALESCE(:before, n.notificationId + 1)                                                                        
              ORDER BY n.notificationId DESC               
              """)
    List<Notification> findByWardIdWithCursor(
            @Param("wardId") Long wardId,
            @Param("since") LocalDateTime since,
            @Param("before") Long before,
            Pageable pageable);

    /**
     * 개인 알림함 — recipient_practitioner_id 직접 매칭.
     * 동일하게 COALESCE 패턴.
     */
    @Query("""                                                                                                                                      
              SELECT n FROM Notification n                                                                                                            
              WHERE n.recipientPractitioner.practitionerId = :practitionerId
                AND n.createdAt >= COALESCE(:since, n.createdAt)                                                                                      
                AND n.notificationId < COALESCE(:before, n.notificationId + 1)
              ORDER BY n.notificationId DESC                                                                                                          
              """)
    List<Notification> findByRecipientPractitionerIdWithCursor(
            @Param("practitionerId") Long practitionerId,
            @Param("since") LocalDateTime since,
            @Param("before") Long before,
            Pageable pageable);
}