package com.ssafy.happynurse.domain.doctor.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_order_id")
    private Long medicationOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient; // 대상 환자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private Encounter encounter; // 입원 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriber_id", nullable = false)
    private Practitioner prescriber; // 처방 의사

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id")
    private Medication medication; // 처방 약물

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // 오더 상태

    @Column(name = "date_written", nullable = false)
    private LocalDateTime dateWritten; // 처방 작성 시간

    @Column(name = "order_code", length = 32)
    private String orderCode; // 처방코드

    @Column(name = "order_name", length = 200)
    private String orderName; // 처방명칭

    @Column(name = "dose", precision = 10, scale = 2)
    private BigDecimal dose; // 1회량

    @Column(name = "frequency")
    private Integer frequency; // 횟수

    @Column(name = "dose_unit", length = 16)
    private String doseUnit; // 단위

    @Column(length = 64)
    private String route; // 용법

    @Column(columnDefinition = "TEXT")
    private String remarks; // 참고사항

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType; // 구분

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markCompleted() {
        this.status = OrderStatus.completed;
    }

    public static MedicationOrder create(
            Patient patient, Encounter encounter, Practitioner prescriber, Medication medication,
            String orderType, String orderCode, String orderName,
            BigDecimal dose, Integer frequency, String doseUnit,
            String route, String remarks) {
        MedicationOrder order = new MedicationOrder();
        order.patient = patient;
        order.encounter = encounter;
        order.prescriber = prescriber;
        order.medication = medication;
        order.orderType = OrderType.valueOf(orderType);
        order.orderCode = orderCode;
        order.orderName = orderName;
        order.dose = dose;
        order.frequency = frequency;
        order.doseUnit = doseUnit;
        order.route = route;
        order.remarks = remarks;
        order.status = OrderStatus.active;
        order.dateWritten = LocalDateTime.now();
        return order;
    }

    public void applyUpdate(OrderStatus status, BigDecimal dose, Integer frequency,
                            String doseUnit, String route, String remarks) {
        if (status != null)    this.status = status;
        if (dose != null)      this.dose = dose;
        if (frequency != null) this.frequency = frequency;
        if (doseUnit != null)  this.doseUnit = doseUnit;
        if (route != null)     this.route = route;
        if (remarks != null)   this.remarks = remarks;
    }
}