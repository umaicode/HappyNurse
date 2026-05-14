package com.ssafy.happynurse.domain.nfc.service;

import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueRequest;
import com.ssafy.happynurse.domain.nfc.dto.NfcTagIssueResponse;
import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import com.ssafy.happynurse.domain.nfc.repository.NfcTagRepository;
import com.ssafy.happynurse.domain.watch.repository.MedicationRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NfcTagIssueService {

    private final NfcTagRepository nfcTagRepository;
    private final MedicationOrderRepository medicationOrderRepository;
    private final MedicationRepository medicationRepository;

    @Transactional
    public NfcTagIssueResponse issue(NfcTagIssueRequest request, String role) {
        // TODO 운영 진입 전 role=="nurse" 가드 복원 — 약물 NFC 발급은 간호사 권한
        if (request.tagType() != TagType.medication) {
            // 환자 팔찌(wristband) 발급은 patient_wristband 테이블 워크플로우가 따로 있어 본 endpoint 에서는 금지
            throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID,
                    "본 endpoint 는 medication 태그 발급만 지원합니다. tagType=" + request.tagType());
        }
        NfcPayload payload = new NfcPayload(request.payloadType(), request.payloadId());
        validateMedicationPayload(payload);

        LocalDateTime now = LocalDateTime.now();

        // 재사용 시나리오: 같은 시리얼의 칩이 이미 등록돼 있으면(폐기된 것 포함) 의미를 덮어씀
        // 처음 보는 시리얼이면 새로 INSERT
        NfcTag tag = nfcTagRepository.findByTagUid(request.tagUid())
                .map(existing -> {
                    existing.reissue(TagType.medication, payload, now);
                    return existing; // 트랜잭션 dirty checking 으로 UPDATE 됨
                })
                .orElseGet(() -> nfcTagRepository.save(
                        NfcTag.issue(request.tagUid(), TagType.medication, payload, now)));

        return NfcTagIssueResponse.from(tag);
    }

    private void validateMedicationPayload(NfcPayload payload) {
        if (payload.isOrder()) {
            medicationOrderRepository.findById(payload.id())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_ORDER_NOT_FOUND,
                            "orderId: " + payload.id()));
        } else if (payload.isDrug()) {
            medicationRepository.findById(payload.id())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_NOT_FOUND,
                            "medicationId: " + payload.id()));
        } else {
            throw new CustomException(ErrorCode.NFC_PAYLOAD_INVALID,
                    "medication 태그는 payloadType 이 ORDER 또는 DRUG 여야 합니다.");
        }
    }
}
