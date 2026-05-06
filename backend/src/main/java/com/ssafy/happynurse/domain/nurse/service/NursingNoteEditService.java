package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.dto.NursingNoteMedicationEditRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordUpdateRequest;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NursingNoteEditService {

    private final NursingRecordService nursingRecordService;
    private final NursingRecordRepository nursingRecordRepository;
    private final MedicationAdministrationService medicationAdministrationService;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final NursingNoteService nursingNoteService;

    public NursingNoteItemResponse updateSttNote(Long nursingRecordId,
                                                 NursingRecordUpdateRequest request,
                                                 Long currentPractitionerId) {
        nursingRecordService.update(nursingRecordId, request, currentPractitionerId);

        NursingRecord refreshed = nursingRecordRepository.findById(nursingRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND));
        return nursingNoteService.buildSttItem(refreshed, currentPractitionerId);
    }

    public NursingNoteItemResponse updateMedication(String taggingId,
                                                    NursingNoteMedicationEditRequest request,
                                                    Long currentPractitionerId) {
        medicationAdministrationService.updateMedicationGroup(
                taggingId, request.medications(), request.confirmedAt(), currentPractitionerId);

        List<MedicationAdministration> refreshed =
                medicationAdministrationRepository.findAllByTaggingId(taggingId);
        return nursingNoteService.buildMedicationItem(refreshed, currentPractitionerId);
    }

    public NursingNoteItemResponse confirmSttNote(Long nursingRecordId, Long currentPractitionerId) {
        nursingRecordService.confirm(nursingRecordId, currentPractitionerId);

        NursingRecord refreshed = nursingRecordRepository.findById(nursingRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND));
        return nursingNoteService.buildSttItem(refreshed, currentPractitionerId);
    }

    public NursingNoteItemResponse confirmMedication(String taggingId, Long currentPractitionerId) {
        medicationAdministrationService.confirm(taggingId, currentPractitionerId);

        List<MedicationAdministration> refreshed =
                medicationAdministrationRepository.findAllByTaggingId(taggingId);
        return nursingNoteService.buildMedicationItem(refreshed, currentPractitionerId);
    }

    public void deleteSttNote(Long nursingRecordId, Long currentPractitionerId) {
        nursingRecordService.delete(nursingRecordId, currentPractitionerId);
    }

    public void deleteMedication(String taggingId, Long currentPractitionerId) {
        medicationAdministrationService.delete(taggingId, currentPractitionerId);
    }

    public NursingNoteItemResponse confirm(String itemId, Long currentPractitionerId) {
        return tryParseLong(itemId)
                .<NursingNoteItemResponse>map(id -> confirmSttNote(id, currentPractitionerId))
                .orElseGet(() -> confirmMedication(itemId, currentPractitionerId));
    }

    public void delete(String itemId, Long currentPractitionerId) {
        tryParseLong(itemId).ifPresentOrElse(
                id -> deleteSttNote(id, currentPractitionerId),
                () -> deleteMedication(itemId, currentPractitionerId));
    }

    private static Optional<Long> tryParseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}