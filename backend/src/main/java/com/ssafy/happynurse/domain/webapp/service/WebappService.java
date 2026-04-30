package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.nurse.entity.Notification;
import com.ssafy.happynurse.domain.nurse.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.repository.NotificationRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.*;
import com.ssafy.happynurse.domain.webapp.entity.InputMethod;
import com.ssafy.happynurse.domain.webapp.entity.PatientSelfReport;
import com.ssafy.happynurse.domain.webapp.entity.QuickSymptomButton;
import com.ssafy.happynurse.domain.webapp.event.SymptomSubmittedEvent;
import com.ssafy.happynurse.domain.webapp.repository.PatientSelfReportRepository;
import com.ssafy.happynurse.domain.webapp.repository.QuickSymptomButtonRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebappService {

    private static final DateTimeFormatter BIRTH_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final QuickSymptomButtonRepository quickSymptomButtonRepository;
    private final PatientSelfReportRepository patientSelfReportRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NfcEntryResponse getPatientEntry(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        return new NfcEntryResponse(
                patientId,
                encounter.getName(),
                encounter.getRoom().getRoomName()
        );
    }

    public PatientVerifyResult verifyPatient(PatientVerifyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        boolean nameMatch = encounter.getName().equals(request.getName());
        boolean birthMatch = encounter.getBirthDate().format(BIRTH_FORMATTER).equals(request.getBirthDate());

        if (!nameMatch || !birthMatch) {
            throw new CustomException(ErrorCode.PATIENT_VERIFY_FAILED);
        }

        String token = jwtTokenProvider.createPatientToken(request.getPatientId(), encounter.getName());

        // NullPointerException 해결
        Practitioner assignedPractitioner = encounter.getAssignedPractitioner();

        return new PatientVerifyResult(
                token,
                request.getPatientId(),
                encounter.getName(),
                encounter.getRoom().getRoomName(),
                encounter.getGender().name(),
                encounter.getDepartmentCode(),
                encounter.getDiseaseName(),
                encounter.getChiefComplaint(),
                encounter.getSurgeryName(),
//                encounter.getAssignedPractitioner().getName()
                // NullPointerException 해결
                assignedPractitioner != null ? assignedPractitioner.getName() : null
        );

    }

    public PatientVerifyResult devVerify(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        log.warn("[DEV] dev-verify issued (verification skipped) — patientId={}", patientId);

        String token = jwtTokenProvider.createPatientToken(patientId, encounter.getName());

        Practitioner assignedPractitioner = encounter.getAssignedPractitioner();
        return new PatientVerifyResult(
                token,
                patientId,
                encounter.getName(),
                encounter.getRoom().getRoomName(),
                encounter.getGender().name(),
                encounter.getDepartmentCode(),
                encounter.getDiseaseName(),
                encounter.getChiefComplaint(),
                encounter.getSurgeryName(),
                assignedPractitioner != null ? assignedPractitioner.getName() : null
        );
    }

    public List<SymptomButtonResponse> getButtons() {
        return quickSymptomButtonRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(SymptomButtonResponse::from)
                .toList();
    }

    @Transactional
    public SymptomSubmitResponse submitSymptom(Long jwtPatientId, Long pathPatientId, SymptomSubmitRequest request) {
        if (!jwtPatientId.equals(pathPatientId)) {
            throw new CustomException(ErrorCode.PATIENT_ID_MISMATCH);
        }

        boolean hasButton = request.getButtonId() != null;
        boolean hasText = request.getSymptomText() != null && !request.getSymptomText().isBlank();
        if (!hasButton && !hasText) {
            throw new CustomException(ErrorCode.SYMPTOM_INPUT_INVALID);
        }

        Patient patient = patientRepository.findById(jwtPatientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        QuickSymptomButton button = null;
        String symptomText;
        InputMethod inputMethod;

        if (hasButton) {
            button = quickSymptomButtonRepository.findById(request.getButtonId())
                    .orElseThrow(() -> new CustomException(ErrorCode.BUTTON_NOT_FOUND));
            symptomText = hasText
                    ? button.getLabel() + " - " + request.getSymptomText()
                    : button.getLabel();
            inputMethod = InputMethod.quick_button;
        } else {
            symptomText = request.getSymptomText();
            inputMethod = InputMethod.text;
        }

        PatientSelfReport savedReport = patientSelfReportRepository.save(PatientSelfReport.create(patient, encounter, inputMethod, button, symptomText));

        Practitioner assignedPractitioner = encounter.getAssignedPractitioner();
        if (assignedPractitioner != null) {
            notificationRepository.save(Notification.create(
                    assignedPractitioner,
                    SourceType.self_report,
                    savedReport,
                    patient,
                    encounter.getName() + "님의 증상 알림",
                    symptomText
            ));
        }

        // 이벤트 발행 (트랜잭션 커밋 후 SseNotificationListener가 처리)
        eventPublisher.publishEvent(new SymptomSubmittedEvent(
                assignedPractitioner != null ? assignedPractitioner.getPractitionerId() : null,
                patient.getPatientId(),
                encounter.getName(),
                encounter.getRoom().getRoomName(),
                symptomText,
                savedReport.getSelfReportId(),
                savedReport.getSubmittedAt()
        ));

        return new SymptomSubmitResponse(savedReport.getSelfReportId(), savedReport.getSubmittedAt());
    }
}