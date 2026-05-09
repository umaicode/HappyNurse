package com.ssafy.happynurse.domain.reminder.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * STT 발화 기반 음성 메모 타이머. 간호사가 워치에 발화한 시각이 되면 워치 풀스크린 알람으로 본문이 뜸.
 * 환자 관련 알람뿐 아니라 일반 업무 알람("2시간 뒤 인수인계 준비")도 동일한 형태로 등록.
 * 발화 텍스트(contentSummary)를 그대로 알람 본문으로 사용.
 */
@Entity
@Table(name = "stt_reminder", indexes = {
        @Index(name = "idx_stt_status_fireat", columnList = "status,fire_at"),
        @Index(name = "idx_stt_practitioner", columnList = "practitioner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SttReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stt_reminder_id")
    private Long sttReminderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "stt_text", columnDefinition = "TEXT", nullable = false)
    private String sttText;

    @Column(name = "content_summary", length = 200, nullable = false)
    private String contentSummary;

    @Column(name = "fire_at", nullable = false)
    private LocalDateTime fireAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SttReminderStatus status;

    /** NULL = 미발사. CAS 마킹 시 not-null 로 채워지면서 status=FIRED 동시 전이. */
    @Column(name = "alert_sent_at")
    private LocalDateTime alertSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SttReminder create(
            Practitioner practitioner,
            Long wardId,
            String sttText,
            String contentSummary,
            LocalDateTime fireAt
    ) {
        SttReminder r = new SttReminder();
        r.practitioner = practitioner;
        r.wardId = wardId;
        r.sttText = sttText;
        r.contentSummary = contentSummary;
        r.fireAt = fireAt;
        r.status = SttReminderStatus.SCHEDULED;
        return r;
    }

    public void cancel() {
        if (this.status == SttReminderStatus.SCHEDULED) {
            this.status = SttReminderStatus.CANCELED;
        }
    }
}
