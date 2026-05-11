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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "iv_infusion", indexes = {
        @Index(name = "idx_iv_status_expected_end", columnList = "status,expected_end_at")
})
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

    @OneToMany(mappedBy = "ivInfusion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC, ivInfusionMedicationId ASC")
    private List<IvInfusionMedication> medications = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner; // 투여 시작 간호사

    @Column(name = "total_volume_ml", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVolumeMl;

    @Column(name = "current_rate_ml_per_hr", nullable = false, precision = 8, scale = 2)
    private BigDecimal currentRateMlPerHr;

    @Enumerated(EnumType.STRING)
    @Column(name = "drop_set", nullable = false, length = 16)
    private DropSet dropSet;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expected_end_at", nullable = false)
    private LocalDateTime expectedEndAt;

    @Column(name = "actual_end_at")
    private LocalDateTime actualEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfusionStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** 종료 5분 전 알림 발사 시각 (NULL = 미발사, 속도 변경 시 NULL 로 리셋되어 재발사 보장) */
    @Column(name = "five_min_alert_sent_at")
    private LocalDateTime fiveMinAlertSentAt;

    /** 종료 알림 발사 시각 (NULL = 미발사, 한 번 발사 후 재발사 안 함) */
    @Column(name = "end_alert_sent_at")
    private LocalDateTime endAlertSentAt;

    /** 일시정지 시각 (PAUSED 상태일 때) */
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ---------- 정적 팩토리 ----------

    /**
     * 신규 수액 시작 {@code expectedEndAt} 자동 계산: {@code now + (totalVolumeMl / rate) hr}
     * 상태는 IN_PROGRESS, 알림 플래그는 NULL
     */
    public static IvInfusion start(
            Patient patient,
            Encounter encounter,
            List<MedicationOrder> orders,
            Practitioner practitioner,
            BigDecimal totalVolumeMl,
            BigDecimal initialRateMlPerHr,
            DropSet dropSet,
            LocalDateTime now,
            String note
    ) {
        if (totalVolumeMl == null || totalVolumeMl.signum() <= 0) {
            throw new IllegalArgumentException("totalVolumeMl must be positive");
        }
        if (initialRateMlPerHr == null || initialRateMlPerHr.signum() <= 0) {
            throw new IllegalArgumentException("initialRateMlPerHr must be positive");
        }
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must contain at least 1 order");
        }
        if (dropSet == null) {
            throw new IllegalArgumentException("dropSet must not be null");
        }
        IvInfusion iv = new IvInfusion();
        iv.patient = patient;
        iv.encounter = encounter;
        iv.medicationOrder = orders.get(0); // primary
        iv.practitioner = practitioner;
        iv.totalVolumeMl = totalVolumeMl;
        iv.currentRateMlPerHr = initialRateMlPerHr;
        iv.dropSet = dropSet;
        iv.startedAt = now;
        iv.expectedEndAt = computeEndAt(now, totalVolumeMl, initialRateMlPerHr);
        iv.status = InfusionStatus.IN_PROGRESS;
        iv.note = note;
        for (int i = 0; i < orders.size(); i++) {
            iv.medications.add(IvInfusionMedication.of(iv, orders.get(i), i + 1));
        }
        return iv;
    }

    // ---------- 비즈니스 메서드 ----------

    /**
     * 주입 속도 변경. 잔여 용량을 현재 시각 기준으로 재계산해 새 속도로 expectedEndAt 갱신
     * 5분 전 알림 플래그를 NULL 로 리셋해 새 종료 시각 기준으로 재발사
     */
    public void changeRate(BigDecimal newRate, LocalDateTime now) {
        if (newRate == null || newRate.signum() <= 0) {
            throw new IllegalArgumentException("newRate must be positive");
        }
        if (this.status != InfusionStatus.IN_PROGRESS) {
            throw new IllegalStateException("only IN_PROGRESS infusion can change rate, but status=" + this.status);
        }
        BigDecimal remaining = remainingVolumeMl(now);
        if (remaining.signum() <= 0) {
            return;
        }
        this.currentRateMlPerHr = newRate;
        this.expectedEndAt = computeEndAt(now, remaining, newRate);
        this.fiveMinAlertSentAt = null;
    }

    /** 속도 변경 요청이 새 dropSet 을 동반하면 저장값 갱신 */
    public void updateDropSet(DropSet dropSet) {
        if (dropSet != null) {
            this.dropSet = dropSet;
        }
    }

    public void markFiveMinAlertSent(LocalDateTime now) {
        this.fiveMinAlertSentAt = now;
    }

    public void markEndAlertSent(LocalDateTime now) {
        this.endAlertSentAt = now;
    }

    /** 수동/자동 종료 */
    public void complete(LocalDateTime now) {
        this.status = InfusionStatus.COMPLETED;
        this.actualEndAt = now;
    }

    // ---------- 계산 헬퍼 ----------

    /** 현재 시각 기준 잔여 용량(mL). expectedEndAt 까지 남은 시간 × 현재 속도 */
    public BigDecimal remainingVolumeMl(LocalDateTime now) {
        long secsLeft = Math.max(0L, Duration.between(now, this.expectedEndAt).getSeconds());
        BigDecimal hoursLeft = BigDecimal.valueOf(secsLeft)
                .divide(BigDecimal.valueOf(3600L), 6, RoundingMode.HALF_UP);
        return this.currentRateMlPerHr.multiply(hoursLeft).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDateTime computeEndAt(LocalDateTime from, BigDecimal volumeMl, BigDecimal rateMlPerHr) {
        BigDecimal hours = volumeMl.divide(rateMlPerHr, 6, RoundingMode.HALF_UP);
        long seconds = hours.multiply(BigDecimal.valueOf(3600L)).longValue();
        return from.plusSeconds(seconds);
    }
}