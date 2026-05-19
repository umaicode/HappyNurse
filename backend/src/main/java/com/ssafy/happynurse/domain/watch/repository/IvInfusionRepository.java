package com.ssafy.happynurse.domain.watch.repository;

import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IvInfusionRepository extends JpaRepository<IvInfusion, Long> {

    /**
     * 폴링 — 종료 5분 전 알림 후보. patient만 fetch (medications 컬렉션은 어댑터의 routing-info 조회에서 fetch)
     * 폴링 결과는 스케줄러 register 에 expectedEndAt + id 만 사용하므로 medications 까지 필요 없음
     */
    @Query("""
            SELECT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            WHERE iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
              AND iv.fiveMinAlertSentAt IS NULL
              AND iv.expectedEndAt >= :fromInclusive
              AND iv.expectedEndAt <  :toExclusive
            """)
    List<IvInfusion> findDueFiveMinAlerts(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive")   LocalDateTime toExclusive);

    @Query("""
            SELECT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            WHERE iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
              AND iv.endAlertSentAt IS NULL
              AND iv.expectedEndAt >= :fromInclusive
              AND iv.expectedEndAt <  :toExclusive
            """)
    List<IvInfusion> findDueEndAlerts(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive")   LocalDateTime toExclusive);

    /** 5분 전 알림 발사 atomic 마킹. race-free CAS */
    @Modifying
    @Query("""
            UPDATE IvInfusion iv
               SET iv.fiveMinAlertSentAt = :now
             WHERE iv.ivInfusionId = :id
               AND iv.fiveMinAlertSentAt IS NULL
               AND iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
            """)
    int markFiveMinAlertSentIfNotSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** 종료 알림 발사 atomic 마킹 + status=COMPLETED, actualEndAt */
    @Modifying
    @Query("""
            UPDATE IvInfusion iv
               SET iv.endAlertSentAt = :now,
                   iv.status         = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.COMPLETED,
                   iv.actualEndAt    = :now
             WHERE iv.ivInfusionId = :id
               AND iv.endAlertSentAt IS NULL
               AND iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
            """)
    int markEndAlertSentIfNotSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 병동 목록 — patient + medications + medication 한 쿼리 fetch
     */
    @Query("""
            SELECT DISTINCT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            LEFT JOIN FETCH iv.medications ivm
            LEFT JOIN FETCH ivm.medication
            JOIN iv.encounter e
            JOIN e.room r
            WHERE r.ward.wardId = :wardId
            ORDER BY iv.startedAt DESC
            """)
    List<IvInfusion> findByWardId(@Param("wardId") Long wardId);

    @Query("""
            SELECT DISTINCT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            LEFT JOIN FETCH iv.medications ivm
            LEFT JOIN FETCH ivm.medication
            JOIN iv.encounter e
            JOIN e.room r
            WHERE r.ward.wardId = :wardId
              AND iv.status = :status
            ORDER BY iv.startedAt DESC
            """)
    List<IvInfusion> findByWardIdAndStatus(
            @Param("wardId") Long wardId,
            @Param("status") InfusionStatus status);

    /**
     * 어댑터 / getDetailByTag 공용 — routing 정보(patient/encounter/room/ward/assignedPractitioner) +
     * 응답 표시용 medications 까지 한 쿼리 fetch
     */
    @Query("""
            SELECT DISTINCT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            JOIN FETCH iv.encounter e
            JOIN FETCH e.room r
            JOIN FETCH r.ward
            LEFT JOIN FETCH e.assignedPractitioner
            LEFT JOIN FETCH iv.medications ivm
            LEFT JOIN FETCH ivm.medication
            WHERE iv.ivInfusionId = :id
            """)
    Optional<IvInfusion> findByIdWithRoutingInfo(@Param("id") Long id);

    /**
     * tag-resolve: medication_order_id 로 IN_PROGRESS IV 조회 (있다면 1건)
     */
    @Query("""
            SELECT DISTINCT iv FROM IvInfusion iv
            JOIN FETCH iv.patient
            JOIN FETCH iv.encounter e
            JOIN FETCH e.room r
            JOIN FETCH r.ward
            LEFT JOIN FETCH e.assignedPractitioner
            LEFT JOIN FETCH iv.medications ivm
            LEFT JOIN FETCH ivm.medication
            WHERE (iv.medicationOrder.medicationOrderId = :orderId
                   OR EXISTS (SELECT 1 FROM IvInfusionMedication ivm2
                              WHERE ivm2.ivInfusion.ivInfusionId = iv.ivInfusionId
                                AND ivm2.medicationOrder.medicationOrderId = :orderId))
              AND iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
            """)
    Optional<IvInfusion> findActiveByMedicationOrderIdWithRoutingInfo(@Param("orderId") Long orderId);

    /** start 시 중복 방지 가드용 — 같은 처방으로 IN_PROGRESS IV 가 이미 있는지 확인 */
    boolean existsByMedicationOrder_MedicationOrderIdAndStatus(Long medicationOrderId, InfusionStatus status);

    /**
     * MA 그룹 → IvInfusion 조회 (IvInfusionMedication 경유).
     * 믹스 IV 의 어떤 orderId 로 조회해도 동일 IvInfusion 을 반환.
     */
    @Query("""
              SELECT iv FROM IvInfusion iv JOIN iv.medications m
              WHERE m.medicationOrder.medicationOrderId = :orderId
              AND iv.status = com.ssafy.happynurse.domain.watch.entity.InfusionStatus.IN_PROGRESS
              """)
    Optional<IvInfusion> findByMedicationOrderId(@Param("orderId") Long orderId);
}
