package com.ssafy.happynurse.domain.patient.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "encounter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "encounter_id")
    private Long encounterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EncounterStatus status; // 내원 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "class_code", nullable = false)
    private ClassCode classCode; // 진료 구분

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart; // 입원 시각

    @Column(name = "period_end")
    private LocalDateTime periodEnd; // 퇴원 시각

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attending_physician_id")
    private Practitioner attendingPhysician; // 주치의

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_practitioner_id")
    private Practitioner assignedPractitioner; // 담당 간호사

    @Column(name = "department_code", length = 32)
    private String departmentCode; // 진료부서 코드

    @Column(name = "disease_name", length = 100)
    private String diseaseName; // 병명

    @Column(name = "chief_complaint", length = 100)
    private String chiefComplaint;  // 주 증상

    @Column(name = "surgery_name", length = 100)
    private String surgeryName;     // 수술명

    @Column(nullable = false, length = 100)
    private String name; // 이름 (반정규화)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // 성별

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; // 생년월일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room; // 호실

    @Column(name = "bed_name", nullable = false, length = 50)
    private String bedName; // 침상

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}