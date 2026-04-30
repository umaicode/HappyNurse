package com.ssafy.happynurse.domain.doctor.service;

import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderItemResponse;
import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
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
    private final PatientRepository patientRepository;

    public MedicationOrderListResponse getOrdersByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        List<MedicationOrder> orders = medicationOrderRepository.findByPatientIdWithPrescriber(patientId);

        List<MedicationOrderItemResponse> items = orders.stream()
                .map(this::toItemResponse)
                .toList();

        return new MedicationOrderListResponse(
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
                mo.getPrescriber().getName()
        );
    }
}
