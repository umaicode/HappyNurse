package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.nurse.dto.NursingNoteItemResponse;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.notification.api.NotificationEnvelope;
import com.ssafy.happynurse.domain.nurse.notification.entity.SourceType;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationAdministrationSseService {

    private final WardEmitterRegistry wardRegistry;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final NursingNoteService nursingNoteService;
    private final EncounterRepository encounterRepository;

    @Transactional(readOnly = true)
    public void send(String taggingId, Long patientId, Long authorPractitionerId) {
        List<MedicationAdministration> group = medicationAdministrationRepository.findAllByTaggingId(taggingId);
        if (group.isEmpty()) {
            log.warn("투약 SSE 스킵 — taggingId={} administration 없음 (이미 삭제됨?)", taggingId);
            return;
        }

        Optional<Long> wardIdOpt = encounterRepository.findCurrentWardIdByPatientId(patientId);
        if (wardIdOpt.isEmpty()) {
            log.warn("투약 SSE 스킵 — patientId={} 활성 입원 없음", patientId);
            return;
        }

        Long wardId = wardIdOpt.get();
        NursingNoteItemResponse payload = nursingNoteService.buildMedicationItem(group, null);
        MedicationAdministration head = group.get(0);

        NotificationEnvelope envelope = new NotificationEnvelope(
                SourceType.medication_admin,                                              // sourceType
                wardId,                                                                    // wardId
                authorPractitionerId,                                                      // assignedPractitionerId
                patientId,                                                                 // patientId
                null,                                                                      // sourceEntityId — 그룹 단위라 단일 Long PK 없음(taggingId는 String)
                "투약 기록 업데이트",                                                           // title
                null,                                                                      // body
                payload,                                                                   // payload (NursingNoteItemResponse type=MEDICATION)
                head.getEffectiveDatetime().atZone(ZoneId.systemDefault()).toInstant(),    // occurredAt
                null,                                                                      // notificationId — Notification DB 미저장
                null,                                                                      // pushPolicy — dispatcher 우회
                null                                                                       // priority — 사용 안 함
        );

        wardRegistry.send(wardId, envelope);
        log.info("투약 SSE 발송 — taggingId={}, wardId={}, count={}",
                taggingId, wardId, group.size());
    }
}