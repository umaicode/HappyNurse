package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.nurseSTT.entity.NursingRecordCorrectionApplied;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "nursing_record",
        indexes = {
                @Index(name = "idx_nursing_record_encounter_status",
                        columnList = "encounter_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(access = AccessLevel.PACKAGE)
public class NursingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nursing_record_id")
    private Long nursingRecordId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "author_practitioner_id", nullable = false)
    private Long authorPractitionerId; // 말한 간호사

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecordStatus status = RecordStatus.draft;

    @Column(name = "audio_file_url", length = 512)
    private String audioFileUrl; // 원본 음성 S3 URL

    @Column(name = "original_stt_content", columnDefinition = "TEXT")
    private String originalSttContent; // 원본 STT 텍스트 (불변)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "editor_state_json", columnDefinition = "JSON")
    private String editorStateJson; // 후보군 및 현재 상태 메타데이터

    @Column(name = "edit_content", columnDefinition = "TEXT")
    private String editContent; // 수정 중인 에디터 상태

    @Column(name = "final_content", columnDefinition = "TEXT")
    private String finalContent; // 최종 확정 내용

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "nursingRecord", cascade = CascadeType.ALL)
    private List<NursingRecordCorrectionApplied> corrections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.confirmedAt == null) {
            this.confirmedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}