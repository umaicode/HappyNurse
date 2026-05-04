package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_administration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_admin_id")
    private Long medicationAdminId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner; // 투약 간호사

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_order_id")
    private MedicationOrder medicationOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordStatus status;

    @Column(name = "effective_datetime", nullable = false)
    private LocalDateTime effectiveDatetime; // 투약 시각

    @Column(name = "dosage_quantity", precision = 10, scale = 3)
    private BigDecimal dosageQuantity; // 투약 용량 수치

    @Column(name = "dosage_unit", length = 16)
    private String dosageUnit; // 투약 단위

    @Column(name = "nfc_tag_verified", nullable = false)
    private Boolean nfcTagVerified; // NFC 태깅 검증 여부

    @Column(name = "tagging_id", nullable = false, length = 36)
    private String taggingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MedicationAdministration ofVerifiedNfc(
            Patient patient,
            Encounter encounter,
            Practitioner practitioner,
            MedicationOrder medicationOrder,
            Medication medication,
            LocalDateTime effectiveDatetime,
            String taggingId
    ) {
        MedicationAdministration ma = new MedicationAdministration();
        ma.patient = patient;
        ma.encounter = encounter;
        ma.practitioner = practitioner;
        ma.medicationOrder = medicationOrder;
        ma.medication = medication;
        ma.status = RecordStatus.confirmed;
        ma.effectiveDatetime = effectiveDatetime;
        ma.dosageQuantity = medicationOrder.getDose();
        ma.dosageUnit = medicationOrder.getDoseUnit();
        ma.nfcTagVerified = true;
        ma.taggingId = taggingId;
        return ma;
    }
}