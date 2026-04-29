package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.nurse.repository.EncounterDraftCount;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.cache.AssignedPatientsCache;
import com.ssafy.happynurse.domain.patient.dto.WardPatientListResponse;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardPatientListService {

    private final EncounterRepository encounterRepository;
    private final NursingRecordRepository nursingRecordRepository;
    private final AssignedPatientsCache assignedPatientsCache;

    public List<WardPatientListResponse> listWardPatients(Long wardId, Long currentPractitionerId) {
        List<Encounter> encounters = encounterRepository.findInProgressByWard(wardId);

        if (encounters.isEmpty()) {
            return List.of();
        }

        List<Long> encounterIds = encounters.stream()
                .map(Encounter::getEncounterId)
                .toList();

        Map<Long, Long> draftCountMap = nursingRecordRepository
                .countDraftByEncounterIds(encounterIds)
                .stream()
                .collect(Collectors.toMap(
                        EncounterDraftCount::getEncounterId,
                        EncounterDraftCount::getCnt));

        Set<Long> mySelection = resolveMySelection(currentPractitionerId, wardId);

        return encounters.stream()
                .map(e -> toResponse(
                        e,
                        draftCountMap.getOrDefault(e.getEncounterId(), 0L),
                        mySelection.contains(e.getEncounterId())))
                .toList();
    }

    private Set<Long> resolveMySelection(Long practitionerId, Long wardId) {
        return assignedPatientsCache.read(practitionerId, wardId)
                .orElseGet(() -> {
                    Set<Long> seed = encounterRepository
                            .findInProgressByWardAndAssignedPractitioner(wardId, practitionerId)
                            .stream()
                            .map(Encounter::getEncounterId)
                            .collect(Collectors.toSet());
                    assignedPatientsCache.write(practitionerId, wardId, seed);
                    return seed;
                });
    }

    private WardPatientListResponse toResponse(Encounter e, long unconfirmedNursingCount, boolean isMyPatient) {
        return new WardPatientListResponse(
                e.getPatient().getPatientId(),
                e.getEncounterId(),
                e.getName(),
                e.getGender(),
                e.getBirthDate(),
                e.getRoom().getRoomName(),
                e.getBedName(),
                unconfirmedNursingCount,
                isMyPatient
        );
    }
}
