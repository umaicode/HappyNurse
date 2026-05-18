package com.ssafy.happynurse.domain.watch.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.watch.dto.ChangeRateRequest;
import com.ssafy.happynurse.domain.watch.dto.IvInfusionListItemResponse;
import com.ssafy.happynurse.domain.watch.dto.IvInfusionResponse;
import com.ssafy.happynurse.domain.watch.dto.StartIvRequest;
import com.ssafy.happynurse.domain.watch.entity.InfusionStatus;
import com.ssafy.happynurse.domain.watch.entity.IvInfusion;
import com.ssafy.happynurse.domain.watch.repository.IvInfusionRepository;
import com.ssafy.happynurse.domain.watch.scheduler.AlertType;
import com.ssafy.happynurse.domain.watch.scheduler.IvAlertScheduler;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.util.ArrayList;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IvInfusionService {

    private final IvInfusionRepository repository;
    private final NfcTagRepository nfcTagRepository;
    private final MedicationOrderRepository medicationOrderRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final IvAlertScheduler scheduler;

    // 간호기록 SSE를 위한 작업
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public IvInfusionResponse start(StartIvRequest req, Long practitionerId) {
        // 1) Encounter 존재 확인 — IV 가 어느 입원에 걸리는지 검증
        Encounter encounter = encounterRepository.findById(req.encounterId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        // 2) verify 통과한 orders 일괄 조회 (중복 제거) + 존재 검증
        List<Long> distinctOrderIds = req.medicationOrderIds().stream().distinct().toList();
        List<MedicationOrder> orders = medicationOrderRepository.findAllById(distinctOrderIds);
        if (orders.size() != distinctOrderIds.size()) {
            throw new CustomException(ErrorCode.MEDICATION_ORDER_NOT_FOUND,
                    "medicationOrderIds 중 일부 없음: 요청=" + distinctOrderIds.size() + "건, 실제=" + orders.size() + "건");
        }

        // 3) 모든 order 가 같은 encounter 에 속하는지 일관성 검증 (IDOR / 데이터 오류 방지)
        for (MedicationOrder o : orders) {
            if (o.getEncounter() == null
                    || !o.getEncounter().getEncounterId().equals(req.encounterId())) {
                throw new CustomException(ErrorCode.MEDICATION_ORDER_PATIENT_MISMATCH,
                        "orderId=" + o.getMedicationOrderId() + " 가 encounterId=" + req.encounterId() + " 와 일치하지 않음");
            }
        }

        // 4) 중복 IV 가드 — orders 중 하나라도 IN_PROGRESS IV 이미 있으면 409 (의료 안전망)
        for (Long orderId : distinctOrderIds) {
            if (repository.existsByMedicationOrder_MedicationOrderIdAndStatus(orderId, InfusionStatus.IN_PROGRESS)) {
                throw new CustomException(ErrorCode.IV_ALREADY_IN_PROGRESS,
                        "orderId=" + orderId + " 가 다른 IV 에서 이미 진행 중");
            }
        }

        // 5) 의료진 lookup
        Practitioner nurse = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        // 6) IvInfusion + components 생성 + 간호기록(N행) 생성
        BigDecimal rate = req.resolveRateMlPerHr();
        LocalDateTime now = LocalDateTime.now();
        String taggingId = UUID.randomUUID().toString();

        IvInfusion iv = IvInfusion.start(
                orders.get(0).getPatient(),
                encounter,
                orders,
                nurse,
                req.totalVolumeMl(),
                rate,
                req.dropSet(),
                now,
                req.note());
        IvInfusion saved = repository.save(iv);

        // 간호기록용 MedicationAdministration N행 (markCompleted 는 호출 안 함)
        List<MedicationAdministration> mas = new ArrayList<>(orders.size());
        for (MedicationOrder order : orders) {
            mas.add(MedicationAdministration.ofIv(
                    orders.get(0).getPatient(),
                    encounter,
                    nurse,
                    order,
                    order.getMedication(),
                    now,
                    taggingId));
        }
        medicationAdministrationRepository.saveAll(mas);

        // SSE 트리거 (AFTER_COMMIT 발사)
        eventPublisher.publishEvent(new MedicationAdministrationSavedEvent(
                taggingId,
                encounter.getEncounterId(),
                orders.get(0).getPatient().getPatientId(),
                nurse.getPractitionerId()));

        afterCommit(() -> {
            scheduler.register(saved, AlertType.FIVE_MIN_BEFORE);
            scheduler.register(saved, AlertType.COMPLETED);
        });

        return IvInfusionResponse.from(saved, now);
    }

    @Transactional
    public IvInfusionResponse changeRateByTag(String tagUid, ChangeRateRequest req) {
        IvInfusion iv = resolveActiveByTagUid(tagUid);
        BigDecimal newRate = req.resolveRateMlPerHr();
        LocalDateTime now = LocalDateTime.now();
        iv.updateDropSet(req.dropSet());
        iv.changeRate(newRate, now);

        afterCommit(() -> {
            scheduler.cancelAll(iv.getIvInfusionId());
            scheduler.register(iv, AlertType.FIVE_MIN_BEFORE);
            scheduler.register(iv, AlertType.COMPLETED);
        });

        return IvInfusionResponse.from(iv, now);
    }

    @Transactional
    public IvInfusionResponse completeByTag(String tagUid) {
        IvInfusion iv = resolveActiveByTagUid(tagUid);
        if (iv.getStatus() != InfusionStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.IV_INVALID_STATE, "현재 상태=" + iv.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        iv.complete(now);

        afterCommit(() -> scheduler.cancelAll(iv.getIvInfusionId()));

        return IvInfusionResponse.from(iv, now);
    }

    public IvInfusionResponse getDetailByTag(String tagUid) {
        IvInfusion iv = resolveActiveByTagUid(tagUid);
        return IvInfusionResponse.from(iv, LocalDateTime.now());
    }

    public List<IvInfusionListItemResponse> listByWard(Long wardId, InfusionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        List<IvInfusion> rows = (status == null)
                ? repository.findByWardId(wardId)
                : repository.findByWardIdAndStatus(wardId, status);
        return rows.stream()
                .map(iv -> IvInfusionListItemResponse.from(iv, now))
                .toList();
    }

    // ---------- helpers ----------

    /**
     * tagUid → 진행 중 IvInfusion 해석
     * 단계: nfc_tag 조회 → ORDER payload 검증 → orderId 로 IN_PROGRESS IV 조회
     */
    private IvInfusion resolveActiveByTagUid(String tagUid) {
        NfcTag tag = nfcTagRepository.findByTagUidAndIsActiveTrue(tagUid)
                .orElseThrow(() -> new CustomException(ErrorCode.NFC_TAG_NOT_FOUND));
        if (tag.getTagType() != TagType.medication) {
            throw new CustomException(ErrorCode.NFC_TAG_NOT_MEDICATION);
        }
        NfcPayload payload = tag.getPayloadJson();
        if (payload == null || !payload.isOrder() || payload.id() == null) {
            throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID, "tagUid=" + tagUid);
        }
        return repository.findActiveByMedicationOrderIdWithRoutingInfo(payload.id())
                .orElseThrow(() -> new CustomException(ErrorCode.IV_INFUSION_NOT_FOUND,
                        "orderId=" + payload.id() + " 로 진행 중 수액 없음"));
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }
}
