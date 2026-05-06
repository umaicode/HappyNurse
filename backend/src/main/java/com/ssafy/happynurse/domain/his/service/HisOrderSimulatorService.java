package com.ssafy.happynurse.domain.his.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.doctor.entity.MedicationOrder;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.repository.MedicationOrderRepository;
import com.ssafy.happynurse.domain.his.dto.HisEncounterResponse;
import com.ssafy.happynurse.domain.his.dto.HisNurseResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderCreateRequest;
import com.ssafy.happynurse.domain.his.dto.HisOrderItemResponse;
import com.ssafy.happynurse.domain.his.dto.HisOrderUpdateRequest;
import com.ssafy.happynurse.domain.his.event.MedicationOrderCreatedEvent;
import com.ssafy.happynurse.domain.his.event.MedicationOrderUpdatedEvent;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.watch.entity.Medication;
import com.ssafy.happynurse.domain.watch.repository.MedicationRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HisOrderSimulatorService {

    private final MedicationOrderRepository medicationOrderRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final PractitionerRepository practitionerRepository;
    private final MedicationRepository medicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(HisOrderCreateRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Encounter encounter = encounterRepository.findById(request.encounterId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        Practitioner prescriber = practitionerRepository.findById(request.prescriberId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        Medication medication = (request.medicationId() == null)
                ? null
                : medicationRepository.findById(request.medicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        MedicationOrder order = MedicationOrder.create(
                patient, encounter, prescriber, medication,
                request.orderType(), request.orderCode(), request.orderName(),
                request.dose(), request.frequency(), request.doseUnit(),
                request.route(), request.remarks());

        MedicationOrder saved = medicationOrderRepository.save(order);

        Long assignedPractitionerId = encounter.getAssignedPractitioner() != null
                ? encounter.getAssignedPractitioner().getPractitionerId() : null;
        Long wardId = encounter.getRoom().getWard().getWardId();

        eventPublisher.publishEvent(new MedicationOrderCreatedEvent(
                saved.getMedicationOrderId(),
                patient.getPatientId(),
                patient.getName(),
                assignedPractitionerId,
                wardId,
                saved.getOrderName(),
                LocalDateTime.now()));

        log.info("[HIS] 신규 처방 INSERT: orderId={}, patient={}, code={}",
                saved.getMedicationOrderId(), patient.getName(), saved.getOrderCode());

        return saved.getMedicationOrderId();
    }

    @Transactional
    public Long update(Long orderId, HisOrderUpdateRequest request) {
        MedicationOrder order = medicationOrderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_ORDER_NOT_FOUND));

        OrderStatus status = (request.status() != null)
                ? OrderStatus.valueOf(request.status()) : null;

        order.applyUpdate(status, request.dose(), request.frequency(),
                request.doseUnit(), request.route(), request.remarks());

        Encounter encounter = order.getEncounter();
        Long assignedPractitionerId = encounter.getAssignedPractitioner() != null
                ? encounter.getAssignedPractitioner().getPractitionerId() : null;
        Long wardId = encounter.getRoom().getWard().getWardId();

        eventPublisher.publishEvent(new MedicationOrderUpdatedEvent(
                order.getMedicationOrderId(),
                order.getPatient().getPatientId(),
                order.getPatient().getName(),
                assignedPractitionerId,
                wardId,
                order.getOrderName(),
                LocalDateTime.now()));

        log.info("[HIS] 처방 변경: orderId={}, patient={}",
                order.getMedicationOrderId(), order.getPatient().getName());

        return order.getMedicationOrderId();
    }

    public List<HisNurseResponse> getNurses() {
        return encounterRepository.findDistinctAssignedPractitionersByInProgress()
                .stream()
                .map(p -> new HisNurseResponse(
                        p.getPractitionerId(),
                        p.getName(),
                        p.getEmployeeNumber()))
                .toList();
    }

    public List<HisEncounterResponse> getEncountersByNurse(Long nurseId) {
        return encounterRepository.findInProgressByAssignedPractitioner(nurseId)
                .stream()
                .map(e -> new HisEncounterResponse(
                        e.getEncounterId(),
                        e.getPatient().getPatientId(),
                        e.getName(),
                        e.getRoom().getRoomName(),
                        e.getBedName()))
                .toList();
    }

    public List<HisOrderItemResponse> getOrdersByEncounter(Long encounterId) {
        return medicationOrderRepository.findByEncounterId(encounterId)
                .stream()
                .map(o -> new HisOrderItemResponse(
                        o.getMedicationOrderId(),
                        o.getOrderType().name(),
                        o.getOrderCode(),
                        o.getOrderName(),
                        o.getDose(),
                        o.getFrequency(),
                        o.getDoseUnit(),
                        o.getRoute(),
                        o.getRemarks()))
                .toList();
    }
}