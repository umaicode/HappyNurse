package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.NursingRecordUpdateRequest;
import com.ssafy.happynurse.domain.nurse.dto.NursingRecordWriteResponse;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.entity.RecordStatus;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NursingRecordService {

    private final NursingRecordRepository nursingRecordRepository;

    public NursingRecordWriteResponse confirm(Long nursingRecordId, Long currentPractitionerId) {
        NursingRecord record = loadAndAuthorize(nursingRecordId, currentPractitionerId);

        if (record.getStatus() != RecordStatus.draft) {
            throw new CustomException(ErrorCode.INVALID_RECORD_STATUS);
        }

        String finalContent = record.getEditContent();
        var confirmedAt = record.getCreatedAt();

        nursingRecordRepository.confirmDraft(nursingRecordId, finalContent, confirmedAt);

        return new NursingRecordWriteResponse(
                nursingRecordId,
                RecordStatus.confirmed,
                finalContent,
                confirmedAt
        );
    }

    public NursingRecordWriteResponse update(Long nursingRecordId,
                                             NursingRecordUpdateRequest request,
                                             Long currentPractitionerId) {
        NursingRecord record = loadAndAuthorize(nursingRecordId, currentPractitionerId);

        if (request.content() != null) {
            if (request.content().isBlank()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (record.getStatus() == RecordStatus.draft) {
                nursingRecordRepository.updateDraftContent(nursingRecordId, request.content());
            } else {
                nursingRecordRepository.updateContentAsAmended(nursingRecordId, request.content());
            }
        }

        if (request.confirmedAt() != null) {
            nursingRecordRepository.updateConfirmedAt(nursingRecordId, request.confirmedAt());
        }

        NursingRecord updated = nursingRecordRepository.findById(nursingRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND));

        String content = updated.getStatus() == RecordStatus.draft
                ? updated.getEditContent()
                : updated.getFinalContent();

        return new NursingRecordWriteResponse(
                updated.getNursingRecordId(),
                updated.getStatus(),
                content,
                updated.getConfirmedAt()
        );
    }

    public void delete(Long nursingRecordId, Long currentPractitionerId) {
        NursingRecord record = loadAndAuthorize(nursingRecordId, currentPractitionerId);
        nursingRecordRepository.delete(record);
    }

    private NursingRecord loadAndAuthorize(Long nursingRecordId, Long currentPractitionerId) {
        NursingRecord record = nursingRecordRepository.findById(nursingRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND));

        if (!record.getAuthorPractitioner().getPractitionerId().equals(currentPractitionerId)) {
            throw new CustomException(ErrorCode.NURSING_RECORD_NOT_AUTHOR);
        }
        return record;
    }
}
