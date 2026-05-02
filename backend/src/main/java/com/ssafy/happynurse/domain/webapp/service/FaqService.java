package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.webapp.dto.FaqItemResponse;
import com.ssafy.happynurse.domain.webapp.dto.FaqListResponse;
import com.ssafy.happynurse.domain.webapp.entity.Faq;
import com.ssafy.happynurse.domain.webapp.entity.FaqIntent;
import com.ssafy.happynurse.domain.webapp.repository.FaqRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final FaqRepository faqRepository;
    private final DiseaseMatcher diseaseMatcher;
    private final IntentOrderingPolicy intentOrderingPolicy;

    public FaqListResponse getFaq(Long jwtPatientId, Long pathPatientId) {
        if (!jwtPatientId.equals(pathPatientId)) {
            throw new CustomException(ErrorCode.PATIENT_ID_MISMATCH);
        }

        Patient patient = patientRepository.findById(jwtPatientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findByPatientAndStatus(patient, EncounterStatus.in_progress)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        String diseaseName = encounter.getDiseaseName();
        String pNorm = diseaseMatcher.normalize(diseaseName);

        List<Faq> candidates = faqRepository.findCandidatesByPatientNorm(pNorm);
        List<String> candidateNorms = candidates.stream().map(Faq::getDiseaseNameNorm).toList();
        Optional<String> bestMatch = diseaseMatcher.findBestMatch(pNorm, candidateNorms);

        if (bestMatch.isEmpty()) {
            return new FaqListResponse(diseaseName, null, List.of());
        }

        String matchedNorm = bestMatch.get();
        List<Faq> matched = candidates.stream()
                .filter(f -> matchedNorm.equals(f.getDiseaseNameNorm()))
                .toList();

        // 인텐트별 1개 row를 LinkedHashMap에 모음 (UNIQUE 제약으로 인텐트별 1행이 정상이지만 방어)
        Map<FaqIntent, Faq> byIntent = new LinkedHashMap<>();
        for (Faq f : matched) {
            byIntent.putIfAbsent(f.getIntent(), f);
        }

        boolean hasSurgery = isNonBlank(encounter.getSurgeryName());
        boolean hasComplaint = isNonBlank(encounter.getChiefComplaint());

        List<FaqIntent> sortedIntents = intentOrderingPolicy.sort(
                List.copyOf(byIntent.keySet()), hasSurgery, hasComplaint);

        List<FaqItemResponse> items = sortedIntents.stream()
                .map(byIntent::get)
                .map(FaqItemResponse::from)
                .toList();

        String matchedFaqDisease = matched.isEmpty() ? null : matched.get(0).getDiseaseName();
        return new FaqListResponse(diseaseName, matchedFaqDisease, items);
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }
}