package com.ssafy.happynurse.domain.nurseSTT.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quick_correction_dictionary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuickCorrectionDictionary {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "correction_id")
  private Long correctionId;

  @Column(name = "stt_word", nullable = false, length = 100)
  private String sttWord; // STT 오인식 단어

  @Column(name = "correct_word", nullable = false, length = 200)
  private String correctWord; // 정식 용어

  @Column(name = "stt_word_normalized", nullable = false, length = 100)
  private String sttWordNormalized; // 공백제거+소문자 정규화

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private CorrectionCategory category = CorrectionCategory.other;

  @Column(name = "department_code", length = 32)
  private String departmentCode; // 특정 진료부서 전용 (NULL=전체)

  @Column(name = "organization_id")
  private Long organizationId; // 병원별 사전 분리 (NULL=공통)

  @Column(name = "usage_count", nullable = false)
  private Integer usageCount = 0;

  @Column(name = "source", nullable = false, length = 20)
  private String source = "system"; // system, feedback, admin

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_by_practitioner_id")
  private Long createdByPractitionerId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "correctionDictionary", cascade = CascadeType.ALL)
  private List<QuickCorrectionSuggestion> suggestions = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.sttWordNormalized = this.sttWord.replace(" ", "").toLowerCase();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
