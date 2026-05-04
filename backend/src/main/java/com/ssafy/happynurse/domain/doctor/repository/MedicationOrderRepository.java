package com.ssafy.happynurse.domain.doctor.repository;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, Long> {

    @Query("SELECT mo FROM MedicationOrder mo "
            + "JOIN FETCH mo.patient "
            + "JOIN FETCH mo.prescriber "
            + "WHERE mo.patient.patientId = :patientId "
            + "AND mo.dateWritten >= :dayStart AND mo.dateWritten < :dayEnd "
            + "ORDER BY mo.dateWritten DESC")
    List<MedicationOrder> findByPatientIdAndDate(
            @Param("patientId") Long patientId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
            SELECT mo FROM MedicationOrder mo
            JOIN FETCH mo.patient
            LEFT JOIN FETCH mo.medication
            WHERE mo.medicationOrderId IN :orderIds
            """)
    List<MedicationOrder> findAllByIdInWithPatient(@Param("orderIds") Collection<Long> orderIds);

    @Query("""
            SELECT mo FROM MedicationOrder mo
            LEFT JOIN FETCH mo.medication m
            WHERE mo.patient.patientId = :patientId
              AND m.medicationId IN :medicationIds
              AND mo.status = com.ssafy.happynurse.domain.doctor.entity.OrderStatus.active
            ORDER BY mo.dateWritten ASC
            """)
    List<MedicationOrder> findActiveByPatientAndMedicationIds(
            @Param("patientId") Long patientId,
            @Param("medicationIds") Collection<Long> medicationIds);

    /**
     * 동시 투약 방지용 락 조회. 호출자는 반드시 ID 오름차순으로 정렬해 넘겨
     * 두 트랜잭션이 항상 같은 순서로 행 락을 획득하도록 한다 (데드락 회피).
     * JOIN FETCH 는 일부 dialect 에서 락 행동이 모호해 의도적으로 제외 — 연관 객체는
     * 같은 트랜잭션 안에서 lazy load 한다 (저장 케이스의 N 은 작아 비용 무시 가능).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT mo FROM MedicationOrder mo
            WHERE mo.medicationOrderId IN :orderIds
            ORDER BY mo.medicationOrderId ASC
            """)
    List<MedicationOrder> lockAllByIdsOrdered(@Param("orderIds") Collection<Long> orderIds);
}