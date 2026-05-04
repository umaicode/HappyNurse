package com.ssafy.happynurse.domain.nurseSTT.entity;

import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "nursing_record_correction_applied")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NursingRecordCorrectionApplied {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "applied_id")
  private Long appliedId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "nursing_record_id2", nullable = false)
  private NursingRecord nursingRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suggestion_id2", nullable = false)
  private QuickCorrectionSuggestion suggestion;

  @Column(name = "practitioner_id2", nullable = false)
  private Long practitionerId;

  @Column(name = "original_word", nullable = false, length = 100)
  private String originalWord; // 치환 전 원본

  @Column(name = "replaced_word", nullable = false, length = 200)
  private String replacedWord; // 치환 후 단어

  @Column(name = "correction_type", nullable = false, length = 20)
  private String correctionType = "manual"; // exact, fuzzy, manual

  @Column(name = "promoted_to_dictionary", nullable = false)
  private Boolean promotedToDictionary = false;

  @Column(name = "applied_at", nullable = false)
  private LocalDateTime appliedAt;

  @PrePersist
  protected void onCreate() {
    this.appliedAt = LocalDateTime.now();
  }
}
