package com.ssafy.happynurse.domain.handover.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_handover")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_id")
    private Long handoverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter; // 입원 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_practitioner_id", nullable = false)
    private Practitioner fromPractitioner; // 인계자

    @Column(name = "auto_summary", columnDefinition = "TEXT")
    private String autoSummary; // 특이사항 자동 요약

    @Column(name = "auto_summary_json", columnDefinition = "jsonb")
    private String autoSummaryJson; // AI 인수인계 JSON (PASS-BAR)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}