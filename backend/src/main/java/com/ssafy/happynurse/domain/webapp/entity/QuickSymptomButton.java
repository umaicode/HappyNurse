package com.ssafy.happynurse.domain.webapp.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_symptom_button")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuickSymptomButton {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "button_id")
    private Long buttonId;

    @Column(nullable = false, length = 100)
    private String label; // "드레싱 교체 요청" 등

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "surgery_type_code", length = 32)
    private String surgeryTypeCode; // 수술유형 스코프

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}