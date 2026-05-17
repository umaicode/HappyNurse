package com.ssafy.happynurse.domain.nurse.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationAdministrationSaveResponse;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyRequest;
import com.ssafy.happynurse.domain.nurse.dto.MedicationVerifyResponse;
import com.ssafy.happynurse.domain.nurse.entity.MedicationAdministration;
import com.ssafy.happynurse.domain.nurse.repository.MedicationAdministrationRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ssafy.happynurse.domain.nurse.event.MedicationAdministrationSavedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final NfcTagRepository nfcTagRepository;
    private final MedicationOrderRepository medicationOrderRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MedicationVerifyResponse verify(MedicationVerifyRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        NfcTag tag = nfcTagRepository.findByTagUidAndIsActiveTrue(request.tagUid())
                .orElseThrow(() -> new CustomException(ErrorCode.NFC_TAG_NOT_FOUND,
                        "tagUid: " + request.tagUid()));

        if (tag.getTagType() != TagType.medication) {
            throw new CustomException(ErrorCode.NFC_TAG_NOT_MEDICATION,
                    "tagUid: " + request.tagUid() + ", tagType: " + tag.getTagType());
        }

        NfcPayload payload = tag.getPayloadJson();
        if (payload == null || payload.id() == null || payload.type() == null) {
            throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID, "tagUid: " + request.tagUid());
        }

        Long verifiedOrderId;
        if (payload.isOrder()) {
            verifiedOrderId = verifyOrderTag(patient.getPatientId(), payload.id());
        } else if (payload.isDrug()) {
            verifiedOrderId = verifyDrugTag(patient.getPatientId(), payload.id());
        } else {
            throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID,
                    "unknown payload type: " + payload.type());
        }

        return MedicationVerifyResponse.success(verifiedOrderId);
    }

    private Long verifyOrderTag(Long patientId, Long orderId) {
        MedicationOrder order = medicationOrderRepository.findAllByIdInWithPatient(List.of(orderId))
                .stream().findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_ORDER_NOT_FOUND,
                        "orderId: " + orderId));
        if (!order.getPatient().getPatientId().equals(patientId)) {
            throw new CustomException(ErrorCode.MEDICATION_VERIFICATION_FAILED,
                    "orderId " + orderId + " does not belong to patient " + patientId);
        }
        // verify 단계에서 조기 거절: 이미 완료/중지된 처방을 들고 약 준비하다 save 에서 실패하는 UX 방지.
        // (실제 동시성 보호는 save 의 PESSIMISTIC_WRITE 락 + 상태 재검증)
        if (order.getStatus() != OrderStatus.active) {
            throw new CustomException(ErrorCode.MEDICATION_ALREADY_ADMINISTERED,
                    "orderId " + orderId + " status: " + order.getStatus());
        }
        return order.getMedicationOrderId();
    }

    private Long verifyDrugTag(Long patientId, Long medicationId) {
        // 동일 medication 으로 active 처방이 여러 건이면 가장 먼저 작성된 것을 매핑.
        // 클라이언트는 같은 약을 두 번 찍으면 같은 orderId 가 돌아올 수 있으니 dedup 책임.
        return medicationOrderRepository
                .findActiveByPatientAndMedicationIds(patientId, List.of(medicationId))
                .stream().findFirst()
                .map(MedicationOrder::getMedicationOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_VERIFICATION_FAILED,
                        "medicationId " + medicationId + " has no active order for patient " + patientId));
    }

    @Transactional
    public MedicationAdministrationSaveResponse saveAdministrations(
            MedicationAdministrationSaveRequest request, Long practitionerId) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));
        Encounter encounter = encounterRepository.findById(request.encounterId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        // 데드락 회피: ID 오름차순으로 정렬한 뒤 PESSIMISTIC_WRITE 락 획득
        // 데드락 차단 : 두 트랜잭션이 같은 처방 집합을 동시에 처리해도 항상 동일한 순서로 락을 잡음, 락 보유 중에 status 를 재검증해 중복 투약을 차단
        List<Long> sortedDistinctIds = request.medicationOrderIds().stream()
                .distinct().sorted().toList();
        Map<Long, MedicationOrder> ordersById = lockOrdersStrict(sortedDistinctIds);

        LocalDateTime now = LocalDateTime.now();
        String taggingId = UUID.randomUUID().toString();
        List<MedicationAdministration> records = new ArrayList<>(sortedDistinctIds.size());

        // 같은 orderId 가 요청에 중복으로 들어와도 INSERT 는 처방당 1건. (락 후 두 번째 처리 시
        // 이미 markCompleted() 로 상태가 바뀌어 status 가드에 걸리는 부수효과 방지)
        for (Long orderId : sortedDistinctIds) {
            MedicationOrder order = ordersById.get(orderId);
            // 락 보유 중 상태 재검증: verify 통과 후 다른 트랜잭션이 먼저 완료시켰을 수 있다.
            if (order.getStatus() != OrderStatus.active) {
                throw new CustomException(ErrorCode.MEDICATION_ALREADY_ADMINISTERED,
                        "orderId " + orderId + " status: " + order.getStatus());
            }
            if (!order.getPatient().getPatientId().equals(patient.getPatientId())) {
                throw new CustomException(ErrorCode.MEDICATION_ORDER_PATIENT_MISMATCH,
                        "orderId " + orderId + " does not belong to patient " + patient.getPatientId());
            }
            if (order.getMedication() == null) {
                throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID,
                        "orderId " + orderId + " has no linked medication");
            }
            records.add(MedicationAdministration.ofVerifiedNfc(
                    patient, encounter, practitioner, order, order.getMedication(), now, taggingId));
            order.markCompleted();
        }

        List<MedicationAdministration> saved = medicationAdministrationRepository.saveAll(records);
        List<Long> savedIds = saved.stream().map(MedicationAdministration::getMedicationAdminId).toList();

        // saveAll 후 비동기 SSE 발송 트리거 (트랜잭션 commit 후 발사)
        eventPublisher.publishEvent(new MedicationAdministrationSavedEvent(
                taggingId,
                encounter.getEncounterId(),
                patient.getPatientId(),
                practitioner.getPractitionerId()
        ));

        return new MedicationAdministrationSaveResponse(taggingId, saved.size(), savedIds);
    }

    private Map<Long, MedicationOrder> lockOrdersStrict(List<Long> sortedIds) {
        Map<Long, MedicationOrder> ordersById = medicationOrderRepository
                .lockAllByIdsOrdered(sortedIds)
                .stream()
                .collect(Collectors.toMap(MedicationOrder::getMedicationOrderId, Function.identity()));
        if (ordersById.size() != sortedIds.size()) {
            String missing = sortedIds.stream()
                    .filter(id -> !ordersById.containsKey(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new CustomException(ErrorCode.MEDICATION_ORDER_NOT_FOUND, "missing orderIds: " + missing);
        }
        return ordersById;
    }
}
