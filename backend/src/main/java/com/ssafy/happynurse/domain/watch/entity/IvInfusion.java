package com.ssafy.happynurse.domain.watch.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "iv_infusion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IvInfusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iv_infusion_id")
    private Long ivInfusionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_order_id", nullable = false)
    private MedicationOrder medicationOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner; // 투여 시작 간호사

    @Column(name = "total_volume_ml", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVolumeMl; // 총 용량 (mL)

    @Column(name = "current_rate_ml_per_hr", nullable = false, precision = 8, scale = 2)
    private BigDecimal currentRateMlPerHr; // 현재 주입 속도 (mL/hr)

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt; // 투여 시작 시각

    @Column(name = "expected_end_at", nullable = false)
    private LocalDateTime expectedEndAt; // 예상 종료 시각

    @Column(name = "actual_end_at")
    private LocalDateTime actualEndAt; // 실제 종료 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfusionStatus status;

    @Column(columnDefinition = "TEXT")
    private String note; // 특이사항 메모

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}