package com.ssafy.happynurse.domain.doctor.service;

import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderItemResponse;
import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationOrderService {

    private final MedicationOrderRepository medicationOrderRepository;
    private final EncounterRepository encounterRepository;

    public MedicationOrderListResponse getOrdersByEncounterId(Long encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        Patient patient = encounter.getPatient();

        List<MedicationOrder> orders = medicationOrderRepository.findByEncounterId(encounterId);

        List<MedicationOrderItemResponse> items = orders.stream()
                .map(this::toItemResponse)
                .toList();

        return new MedicationOrderListResponse(
                encounter.getEncounterId(),
                patient.getPatientId(),
                patient.getName(),
                items.size(),
                items
        );
    }

    private MedicationOrderItemResponse toItemResponse(MedicationOrder mo) {
        return new MedicationOrderItemResponse(
                mo.getMedicationOrderId(),
                mo.getOrderType(),
                mo.getOrderCode(),
                mo.getOrderName(),
                mo.getDose(),
                mo.getFrequency(),
                mo.getDoseUnit(),
                mo.getRoute(),
                mo.getRemarks(),
                mo.getStatus(),
                mo.getDateWritten(),
                mo.getPrescriber().getPractitionerId(),
                mo.getPrescriber().getName(),
                mo.getCreatedAt(),
                mo.getUpdatedAt()
        );
    }
}