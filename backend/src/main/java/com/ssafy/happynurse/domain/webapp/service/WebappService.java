package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.*;
import com.ssafy.happynurse.domain.webapp.dto.EncounterContext;
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
    private final ApplicationEventPublisher eventPublisher;
    private final SymptomClassificationService classificationService;

    public NfcEntryResponse getPatientEntry(String token) {
        Patient patient = patientRepository.findByNfcToken(token)
            .orElseThrow(() -> new CustomException(ErrorCode.NFC_TOKEN_INVALID));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
            .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        return new NfcEntryResponse(
            patient.getPatientId(),
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

        PatientSelfReport savedReport = patientSelfReportRepository.save(
                PatientSelfReport.create(patient, encounter, inputMethod, button, symptomText));

        Practitioner assignedPractitioner = encounter.getAssignedPractitioner();

        SymptomClassificationService.SymptomClassificationResult classification = hasButton
                ? classificationService.classifyButton(button.getLabel())
                : classificationService.classify(symptomText, EncounterContext.from(encounter));

        // 이벤트 발행 — SymptomSubmittedNotificationAdapter 가 트랜잭션 커밋 후 dispatcher 통해 알림 영속화 + 채널 발사
        eventPublisher.publishEvent(new SymptomSubmittedEvent(
                assignedPractitioner != null ? assignedPractitioner.getPractitionerId() : null,
                patient.getPatientId(),
                encounter.getName(),
                encounter.getRoom().getRoomName(),
                symptomText,
                savedReport.getSelfReportId(),
                savedReport.getSubmittedAt(),
                classification.priority()
        ));

        return new SymptomSubmitResponse(savedReport.getSelfReportId(), savedReport.getSubmittedAt());
    }

}