package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.entity.NursingRecord;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.repository.NursingRecordRepository;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NursingRecordSseService {

    private final WardEmitterRegistry wardRegistry;
    private final NursingRecordRepository nursingRecordRepository;
    private final NursingNoteService nursingNoteService;
    private final EncounterRepository encounterRepository;

    @Transactional(readOnly = true)
    public void send(Long nursingRecordId) {
        NursingRecord record = nursingRecordRepository.findById(nursingRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND));

        Optional<Long> wardIdOpt = encounterRepository.findCurrentWardIdByPatientId(record.getPatientId());
        if (wardIdOpt.isEmpty()) {
            log.warn("간호기록 SSE 스킵 — patientId={} 활성 입원 없음", record.getPatientId());
            return;
        }

        Long wardId = wardIdOpt.get();
        NursingNoteItemResponse payload = nursingNoteService.buildSttItem(record, null);

        // occurredAt: confirm 후면 confirmedAt, 그 외(draft)면 createdAt
        // updatedAt은 Repository의 JPQL 벌크 UPDATE가 @PreUpdate 콜백을 우회하므로 신뢰 불가 — 사용하지 않음
        LocalDateTime occurredLocal = record.getConfirmedAt() != null
                ? record.getConfirmedAt()
                : record.getCreatedAt();

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.nursing_record,
                wardId,
                record.getAuthorPractitionerId(),
                record.getPatientId(),
                nursingRecordId,
                "간호 기록 업데이트",
                null,
                payload,
                occurredLocal.atZone(ZoneId.systemDefault()).toInstant(),
                null,   // notificationId — Notification DB 미저장
                null,   // pushPolicy — dispatcher 우회
                null    // priority — 사용 안 함
        );

        wardRegistry.send(wardId, envelope);
        log.info("간호기록 SSE 발송 — nursingRecordId={}, wardId={}, status={}",
                nursingRecordId, wardId, record.getStatus());
    }
}