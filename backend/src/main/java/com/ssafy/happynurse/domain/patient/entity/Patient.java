package com.ssafy.happynurse.domain.patient.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_room_id")
    private Room currentRoom; // 현재 입원위치

    @Column(name = "current_bed_name", length = 50)
    private String currentBedName; // 현재 베드

    @Column(name = "identifier_value", nullable = false, length = 64)
    private String identifierValue; // 환자 등록번호 (MRN)

    @Column(nullable = false, length = 100)
    private String name; // 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // 성별

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; // 생년월일

    @Column(length = 32)
    private String phone; // 연락처

    @Column(length = 500)
    private String address; // 환자 주소

    @Column(nullable = false)
    private Boolean active; // 환자 활성 상태

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}