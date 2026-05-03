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
     * 도메인 규약: ward 알림함은 "현재 입원 중인 환자의 알림" 만 노출 (퇴원 환자 알림은 ward 추적 불가).
     * timer 같은 patient null 알림은 join 으로 자연스럽게 제외 (PushPolicy.PERSONAL_INFO 정책과 일관).
     */
    @Query("""                                                                                                                                      
              SELECT n FROM Notification n                                                                                                            
              JOIN n.patient p                                    
              JOIN Encounter e ON e.patient = p                                                                                                       
                  AND e.status = com.ssafy.happynurse.domain.patient.entity.EncounterStatus.in_progress                                               
              WHERE e.room.ward.wardId = :wardId                                                                                                      
                AND (:since IS NULL OR n.createdAt >= :since)                                                                                         
                AND (:before IS NULL OR n.notificationId < :before)                                                                                   
              ORDER BY n.notificationId DESC                      
              """)
    List<Notification> findByWardIdWithCursor(
            @Param("wardId") Long wardId,
            @Param("since") LocalDateTime since,
            @Param("before") Long before,
            Pageable pageable);

    /**
     * 개인 알림함 — recipient_practitioner_id 직접 매칭.
     * ward 권한 없는 직무 / 모바일 앱이 사용.
     */
    @Query("""                                                                                                                                      
              SELECT n FROM Notification n                                                                                                            
              WHERE n.recipientPractitioner.practitionerId = :practitionerId
                AND (:since IS NULL OR n.createdAt >= :since)                                                                                         
                AND (:before IS NULL OR n.notificationId < :before)
              ORDER BY n.notificationId DESC                                                                                                          
              """)
    List<Notification> findByRecipientPractitionerIdWithCursor(
            @Param("practitionerId") Long practitionerId,
            @Param("since") LocalDateTime since,
            @Param("before") Long before,
            Pageable pageable);
}