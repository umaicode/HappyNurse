package com.ssafy.happynurse.domain.nurse.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient; // 환자 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter; // 입원 내역

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_practitioner_id", nullable = false)
    private Practitioner authorPractitioner; // 말한 간호사

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    @Column(nullable = false, length = 255)
    private String version; // 락

    @Column(name = "audio_file_url", length = 512)
    private String audioFileUrl; // 원본 음성 S3 url (수동 입력 시 NULL)

    @Column(name = "original_stt_content", columnDefinition = "TEXT")
    private String originalSttContent; // 원본 STT 텍스트 (불변, 수동 입력 시 NULL)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "editor_state_json", columnDefinition = "JSON")
    private String editorStateJson; // 후보군 및 현재 상태 메타데이터

    @Column(name = "edit_content", columnDefinition = "TEXT")
    private String editContent; // 수정 중인 에디터 상태 (임시저장)

    @Column(name = "final_content", columnDefinition = "TEXT")
    private String finalContent; // 최종 확정 내용

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt; // 확정 시간

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}