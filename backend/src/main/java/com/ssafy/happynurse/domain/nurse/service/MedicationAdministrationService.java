package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationWriteResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationDosageEditItem;
import com.ssafy.happynurse.domain.nurse.dto.MedicationItemResponse;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurseSTT.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicationAdministrationService {

    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MedicationAdministrationWriteResponse confirm(String taggingId, Long currentPractitionerId) {
        List<MedicationAdministration> group = loadGroupAndAuthorize(taggingId, currentPractitionerId);

        if (group.get(0).getStatus() != RecordStatus.draft) {
            throw new CustomException(ErrorCode.INVALID_RECORD_STATUS);
        }

        medicationAdministrationRepository.confirmByTaggingId(taggingId);

        // confirm DB UPDATE 후 비동기 SSE 발송 트리거 (트랜잭션 commit 후 발사)
        MedicationAdministration head = group.get(0);
        eventPublisher.publishEvent(new MedicationAdministrationSavedEvent(
                taggingId,
                head.getEncounter().getEncounterId(),
                head.getPatient().getPatientId(),
                currentPractitionerId
        ));

        List<MedicationAdministration> refreshed = medicationAdministrationRepository.findAllByTaggingId(taggingId);
        return toResponse(taggingId, refreshed);
    }

    public void updateMedicationGroup(String taggingId,
                                      List<MedicationDosageEditItem> items,
                                      LocalDateTime confirmedAt,
                                      Long currentPractitionerId) {
        boolean hasItems = items != null && !items.isEmpty();
        if (!hasItems && confirmedAt == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<MedicationAdministration> group = loadGroupAndAuthorize(taggingId, currentPractitionerId);

        if (hasItems) {
            Set<Long> groupIds = group.stream()
                    .map(MedicationAdministration::getMedicationAdminId)
                    .collect(Collectors.toSet());

            for (MedicationDosageEditItem item : items) {
                if (!groupIds.contains(item.medicationAdminId())) {
                    throw new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_IN_GROUP);
                }
            }

            for (MedicationDosageEditItem item : items) {
                medicationAdministrationRepository.updateDosageQuantity(
                        item.medicationAdminId(), item.dosageQuantity());
            }
        }

        if (confirmedAt != null) {
            medicationAdministrationRepository.updateEffectiveDatetimeByTaggingId(taggingId, confirmedAt);
        }
    }

    public void delete(String taggingId, Long currentPractitionerId) {
        loadGroupAndAuthorize(taggingId, currentPractitionerId);
        medicationAdministrationRepository.deleteByTaggingId(taggingId);
    }

    private List<MedicationAdministration> loadGroupAndAuthorize(String taggingId, Long currentPractitionerId) {
        List<MedicationAdministration> group = medicationAdministrationRepository.findAllByTaggingId(taggingId);
        if (group.isEmpty()) {
            throw new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_FOUND);
        }
        Long authorId = group.get(0).getPractitioner().getPractitionerId();
        if (!authorId.equals(currentPractitionerId)) {
            throw new CustomException(ErrorCode.MEDICATION_ADMIN_NOT_AUTHOR);
        }
        return group;
    }

    private MedicationAdministrationWriteResponse toResponse(String taggingId, List<MedicationAdministration> group) {
        MedicationAdministration head = group.get(0);
        List<MedicationItemResponse> medications = group.stream()
                .map(this::toMedicationItem)
                .toList();

        return new MedicationAdministrationWriteResponse(
                taggingId,
                head.getStatus(),
                head.getEffectiveDatetime(),
                medications
        );
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
}
