package com.ssafy.happynurse.domain.webapp.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "faq",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_faq_disease_norm_intent",
                columnNames = {"disease_name_norm", "intent"}),
        indexes = @Index(name = "idx_faq_disease_norm", columnList = "disease_name_norm")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long faqId;

    @Column(name = "disease_category", nullable = false, length = 50)
    private String diseaseCategory;

    @Column(name = "disease_name", nullable = false, length = 100)
    private String diseaseName;

    @Column(name = "disease_name_norm", nullable = false, length = 100)
    private String diseaseNameNorm;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 20)
    private FaqIntent intent;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "source_faq_id")
    private Long sourceFaqId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 테스트/적재용 정적 팩토리 (운영 코드에선 사용하지 않음, SQL 시드로 적재)
    public static Faq of(String diseaseCategory, String diseaseName, String diseaseNameNorm,
                         FaqIntent intent, String answer, Long sourceFaqId) {
        Faq faq = new Faq();
        faq.diseaseCategory = diseaseCategory;
        faq.diseaseName = diseaseName;
        faq.diseaseNameNorm = diseaseNameNorm;
        faq.intent = intent;
        faq.answer = answer;
        faq.sourceFaqId = sourceFaqId;
        return faq;
    }
}
