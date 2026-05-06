package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.nurse.dto.MedicationItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemType;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NursingNoteService {

    private final EncounterRepository encounterRepository;
    private final NursingRecordRepository nursingRecordRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final PractitionerRepository practitionerRepository;

    public List<NursingNoteItemResponse> getNursingNotes(Long encounterId,
                                                        LocalDate date,
                                                        Long currentPractitionerId,
                                                        Long currentWardId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        if (!encounter.getRoom().getWard().getWardId().equals(currentWardId)) {
            throw new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<NursingRecord> notes = nursingRecordRepository
                .findAllByEncounterIdAndDateWithAuthor(encounterId, dayStart, dayEnd);
        List<MedicationAdministration> meds = medicationAdministrationRepository
                .findAllByEncounterIdAndDateWithFetch(encounterId, dayStart, dayEnd);

        List<Long> authorIds = notes.stream()
                .map(NursingRecord::getAuthorPractitionerId)
                .distinct()
                .toList();
        Map<Long, Practitioner> authorById = practitionerRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Practitioner::getPractitionerId, p -> p));

        List<NursingNoteItemResponse> items = new ArrayList<>(notes.size() + meds.size());
        for (NursingRecord nr : notes) {
            items.add(toSttItem(nr, currentPractitionerId, authorById));
        }
        addMedicationItems(meds, currentPractitionerId, items);

        items.sort(Comparator
                .comparing(NursingNoteItemResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NursingNoteItemResponse::type)
                .thenComparing(this::secondaryId, Comparator.reverseOrder()));

        return items;
    }

    public List<NursingNoteItemResponse> getDraftNursingNotes(Long encounterId,
                                                             Long currentPractitionerId,
                                                             Long currentWardId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        if (!encounter.getRoom().getWard().getWardId().equals(currentWardId)) {
            throw new CustomException(ErrorCode.ENCOUNTER_NOT_IN_MY_WARD);
        }

        List<NursingRecord> notes = nursingRecordRepository.findAllDraftsByEncounterId(encounterId);
        List<MedicationAdministration> meds = medicationAdministrationRepository
                .findAllDraftsByEncounterIdWithFetch(encounterId);

        List<Long> authorIds = notes.stream()
                .map(NursingRecord::getAuthorPractitionerId)
                .distinct()
                .toList();
        Map<Long, Practitioner> authorById = practitionerRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Practitioner::getPractitionerId, p -> p));

        List<NursingNoteItemResponse> items = new ArrayList<>(notes.size() + meds.size());
        for (NursingRecord nr : notes) {
            items.add(toSttItem(nr, currentPractitionerId, authorById));
        }
        addMedicationItems(meds, currentPractitionerId, items);

        items.sort(Comparator
                .comparing(NursingNoteItemResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NursingNoteItemResponse::type)
                .thenComparing(this::secondaryId, Comparator.reverseOrder()));

        return items;
    }

    public NursingNoteItemResponse buildSttItem(NursingRecord nr, Long currentPractitionerId) {
        Long authorId = nr.getAuthorPractitionerId();
        Practitioner author = practitionerRepository.findById(authorId).orElse(null);
        Map<Long, Practitioner> authorById = author == null ? Map.of() : Map.of(authorId, author);
        return toSttItem(nr, currentPractitionerId, authorById);
    }

    public NursingNoteItemResponse buildMedicationItem(List<MedicationAdministration> group,
                                                       Long currentPractitionerId) {
        if (group == null || group.isEmpty()) return null;

        MedicationAdministration head = group.get(0);
        boolean editable = currentPractitionerId != null
                && head.getPractitioner().getPractitionerId().equals(currentPractitionerId);

        List<MedicationItemResponse> medItems = group.stream()
                .map(this::toMedicationItem)
                .toList();

        return new NursingNoteItemResponse(
                NursingNoteItemType.MEDICATION,
                head.getEffectiveDatetime(),
                head.getStatus(),
                head.getPractitioner().getPractitionerId(),
                head.getPractitioner().getName(),
                editable,
                null,
                null,
                head.getTaggingId(),
                head.getNfcTagVerified(),
                medItems
        );
    }

    private NursingNoteItemResponse toSttItem(NursingRecord nr,
                                              Long currentPractitionerId,
                                              Map<Long, Practitioner> authorById) {
        boolean confirmedOrAmended = nr.getStatus() != RecordStatus.draft;
        LocalDateTime occurredAt = nr.getConfirmedAt();
        String content = confirmedOrAmended ? nr.getFinalContent() : nr.getEditContent();
        Long authorId = nr.getAuthorPractitionerId();
        boolean editable = currentPractitionerId != null && authorId.equals(currentPractitionerId);
        Practitioner author = authorById.get(authorId);

        return new NursingNoteItemResponse(
                NursingNoteItemType.STT_NOTE,
                occurredAt,
                nr.getStatus(),
                authorId,
                author != null ? author.getName() : null,
                editable,
                nr.getNursingRecordId(),
                content,
                null,
                null,
                null
        );
    }

    private void addMedicationItems(List<MedicationAdministration> meds,
                                    Long currentPractitionerId,
                                    List<NursingNoteItemResponse> sink) {
        if (meds.isEmpty()) return;

        Map<String, List<MedicationAdministration>> grouped = meds.stream()
                .collect(Collectors.groupingBy(
                        MedicationAdministration::getTaggingId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        grouped.forEach((taggingId, group) -> {
            NursingNoteItemResponse item = buildMedicationItem(group, currentPractitionerId);
            if (item != null) sink.add(item);
        });
    }

    private MedicationItemResponse toMedicationItem(MedicationAdministration ma) {
        MedicationOrder mo = ma.getMedicationOrder();
        return new MedicationItemResponse(
                ma.getMedicationAdminId(),
                ma.getMedication().getProductCode(),
                ma.getMedication().getProductName(),
                ma.getDosageQuantity(),
                ma.getDosageUnit(),
                mo != null ? mo.getFrequency() : null,
                mo != null ? mo.getRoute() : null
        );
    }

    private long secondaryId(NursingNoteItemResponse item) {
        if (item.type() == NursingNoteItemType.STT_NOTE) {
            return item.nursingRecordId() != null ? item.nursingRecordId() : 0L;
        }
        List<MedicationItemResponse> medications = item.medications();
        if (medications == null || medications.isEmpty()) return 0L;
        Long id = medications.get(0).medicationAdminId();
        return id != null ? id : 0L;
    }
}
