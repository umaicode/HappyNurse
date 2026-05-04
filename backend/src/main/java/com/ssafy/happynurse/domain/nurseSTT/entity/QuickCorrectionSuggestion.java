package com.ssafy.happynurse.domain.nurseSTT.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quick_correction_suggestion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuickCorrectionSuggestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "suggestion_id")
  private Long suggestionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correction_id", nullable = false)
  private QuickCorrectionDictionary correctionDictionary;

  @Column(name = "suggested_word", nullable = false, length = 200)
  private String suggestedWord; // 제안 치환 단어

  @Column(name = "medication_id")
  private Long medicationId; // 약물 치환인 경우 FK

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
