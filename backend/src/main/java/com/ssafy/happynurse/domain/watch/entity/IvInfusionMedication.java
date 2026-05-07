package com.ssafy.happynurse.domain.watch.entity;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iv_infusion_medication",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_iv_infusion_order",
                columnNames = {"iv_infusion_id", "medication_order_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IvInfusionMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iv_infusion_medication_id")
    private Long ivInfusionMedicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iv_infusion_id", nullable = false)
    private IvInfusion ivInfusion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_order_id", nullable = false)
    private MedicationOrder medicationOrder;

    /** 표시 순서 (1-based). */
    @Column(name = "sequence")
    private Integer sequence;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public static IvInfusionMedication of(IvInfusion ivInfusion, MedicationOrder order, Integer sequence) {
        IvInfusionMedication c = new IvInfusionMedication();
        c.ivInfusion = ivInfusion;
        c.medicationOrder = order;
        c.medication = order.getMedication();
        c.sequence = sequence;
        return c;
    }
}
