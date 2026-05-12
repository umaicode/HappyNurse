package com.ssafy.happynurse.domain.handover.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_summary_json", columnDefinition = "jsonb")
    private Map<String, Object> autoSummaryJson; // AI 인수인계 JSON (PASS-BAR)

    // 인수자 확인 체크리스트 상태
    // 키 포맷: "{slot_key}.{item_index}" (현재는 synthesis 만 사용), 값: { "by": <practitionerId>, "at": <isoDateTime> }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checked_items_json", columnDefinition = "jsonb")
    private Map<String, Object> checkedItemsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
