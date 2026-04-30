package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.patient.cache.AssignedPatientsCache;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateRequest;
import com.ssafy.happynurse.domain.patient.dto.AssignedPatientUpdateResponse;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignedPatientService {

    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final AssignedPatientsCache assignedPatientsCache;

    public AssignedPatientUpdateResponse updateMyAssignedPatients(
            Long practitionerId,
            Long wardId,
            AssignedPatientUpdateRequest request) {

        Set<Long> requested = new LinkedHashSet<>(
                request.encounterIds() == null ? List.of() : request.encounterIds());

        List<Encounter> validEncounters = requested.isEmpty()
                ? List.of()
                : encounterRepository.findAllByIdInAndWardAndInProgress(requested, wardId);

        if (validEncounters.size() != requested.size()) {
            throw new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
        }

        int overwroteFromOthersCount = (int) validEncounters.stream()
                .filter(e -> {
                    Practitioner current = e.getAssignedPractitioner();
                    return current != null && !current.getPractitionerId().equals(practitionerId);
                })
                .count();

        Set<Long> dbCurrentlyMine = encounterRepository
                .findInProgressByWardAndAssignedPractitioner(wardId, practitionerId)
                .stream()
                .map(Encounter::getEncounterId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Long> releasedIds = dbCurrentlyMine.stream()
                .filter(id -> !requested.contains(id))
                .toList();

        if (!requested.isEmpty()) {
            Practitioner nurseRef = practitionerRepository.getReferenceById(practitionerId);
            encounterRepository.assignNurseToEncounters(nurseRef, requested);
        }
        if (!releasedIds.isEmpty()) {
            encounterRepository.unassignNurseWhereStillOwned(releasedIds, practitionerId);
        }

        assignedPatientsCache.write(practitionerId, wardId, requested);

        return new AssignedPatientUpdateResponse(
                List.copyOf(requested),
                releasedIds,
                overwroteFromOthersCount);
    }
}
