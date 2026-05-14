package com.ssafy.happynurse.domain.reminder.repository;

import com.ssafy.happynurse.domain.reminder.entity.SttReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SttReminderRepository extends JpaRepository<SttReminder, Long> {

    /**
     * 폴링 — fireAt 이 [from, to) 윈도우 안에 들어오는 SCHEDULED 알림 후보.
     * practitioner 만 fetch (어댑터에서 다시 routing-info 조회로 patient 까지 fetch).
     */
    @Query("""
            SELECT r FROM SttReminder r
            JOIN FETCH r.practitioner
            WHERE r.status = com.ssafy.happynurse.domain.reminder.entity.SttReminderStatus.SCHEDULED
              AND r.alertSentAt IS NULL
              AND r.fireAt >= :fromInclusive
              AND r.fireAt <  :toExclusive
            """)
    List<SttReminder> findDueAlerts(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive")   LocalDateTime toExclusive);

    /** 알림 발사 atomic CAS — race-free. status=SCHEDULED & alertSentAt IS NULL 조건이면 1행 affected. */
    @Modifying
    @Query("""
            UPDATE SttReminder r
               SET r.alertSentAt = :now,
                   r.status      = com.ssafy.happynurse.domain.reminder.entity.SttReminderStatus.FIRED
             WHERE r.sttReminderId = :id
               AND r.alertSentAt IS NULL
               AND r.status      = com.ssafy.happynurse.domain.reminder.entity.SttReminderStatus.SCHEDULED
            """)
    int markAlertSentIfNotSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** 어댑터 — 알림 본문 빌드용 routing 정보 fetch. */
    @Query("""
            SELECT r FROM SttReminder r
            JOIN FETCH r.practitioner
            WHERE r.sttReminderId = :id
            """)
    Optional<SttReminder> findByIdWithRoutingInfo(@Param("id") Long id);

    /** 본인이 등록한 미발사(SCHEDULED) 알람 — 워치 홈 카드 리스트용. fireAt 오름차순. */
    @Query("""
            SELECT r FROM SttReminder r
            WHERE r.practitioner.practitionerId = :practitionerId
              AND r.status = com.ssafy.happynurse.domain.reminder.entity.SttReminderStatus.SCHEDULED
            ORDER BY r.fireAt ASC
            """)
    List<SttReminder> findScheduledByPractitionerId(@Param("practitionerId") Long practitionerId);
}
