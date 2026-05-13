package com.ssafy.happynurse.domain.his.service;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.his.dto.HisEncounterCreateRequest;
import com.ssafy.happynurse.domain.patient.entity.Encounter;
import com.ssafy.happynurse.domain.patient.entity.EncounterStatus;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.entity.Room;
import com.ssafy.happynurse.domain.patient.repository.EncounterRepository;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import com.ssafy.happynurse.domain.patient.repository.RoomRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HisEncounterSimulatorService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final RoomRepository roomRepository;
    private final PractitionerRepository practitionerRepository;

    @Transactional
    public Long admit(HisEncounterCreateRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Practitioner attendingPhysician = (request.attendingPhysicianId() == null) ? null
                : practitionerRepository.findById(request.attendingPhysicianId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        Practitioner assignedPractitioner = (request.assignedPractitionerId() == null) ? null
                : practitionerRepository.findById(request.assignedPractitionerId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        Encounter encounter = Encounter.admit(
                patient, request.classCode(), room, request.bedName(),
                attendingPhysician, assignedPractitioner,
                request.departmentCode(), request.diseaseName(),
                request.chiefComplaint(), request.surgeryName());

        Encounter saved = encounterRepository.save(encounter);

        log.info("[HIS] 입원 INSERT: encounterId={}, patient={}, room={}/{}",
                saved.getEncounterId(), patient.getName(),
                room.getRoomName(), saved.getBedName());

        return saved.getEncounterId();
    }

    @Transactional
    public Long discharge(Long encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENCOUNTER_NOT_FOUND));

        if (encounter.getStatus() == EncounterStatus.finished) {
            throw new CustomException(ErrorCode.ENCOUNTER_ALREADY_DISCHARGED);
        }

        encounter.discharge();

        log.info("[HIS] 퇴원 처리: encounterId={}, patient={}",
                encounter.getEncounterId(), encounter.getName());

        return encounter.getEncounterId();
    }
}
