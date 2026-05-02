package com.ssafy.happynurse.domain.webapp.entity;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_self_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientSelfReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "self_report_id")
    private Long selfReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_method", nullable = false)
    private InputMethod inputMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quick_button_id")
    private QuickSymptomButton quickSymptomButton;

    @Column(name = "symptom_text", nullable = false, columnDefinition = "TEXT")
    private String symptomText; // 증상 직접 입력

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt; // 증상 전송 시간

    public static PatientSelfReport create(Patient patient, Encounter encounter, InputMethod inputMethod, QuickSymptomButton quickSymptomButton, String symptomText) {

        PatientSelfReport report = new PatientSelfReport();
        report.patient = patient;
        report.encounter = encounter;
        report.inputMethod = inputMethod;
        report.quickSymptomButton = quickSymptomButton;
        report.symptomText = symptomText;
        report.submittedAt = LocalDateTime.now();
        return report;
    }
}