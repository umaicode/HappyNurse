package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AppProfileResponse;
import com.ssafy.happynurse.domain.auth.dto.PractitionerMeResponse;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.patient.entity.Organization;
import com.ssafy.happynurse.domain.patient.repository.OrganizationRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PractitionerService {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final OrganizationRepository organizationRepository;

    public PractitionerMeResponse getMyInfo(Long practitionerId, Long wardId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        PractitionerRole role = practitionerRoleRepository
                .findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, wardId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_ROLE_NOT_FOUND));

        return new PractitionerMeResponse(
                practitioner.getPractitionerId(),
                practitioner.getName(),
                practitioner.getEmployeeNumber(),
                role.getRoleCode().name(),
                role.getWard().getWardId(),
                role.getWard().getWardName()
        );
    }

    public AppProfileResponse getAppProfile(Long practitionerId, Long wardId, Long organizationId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_NOT_FOUND));

        PractitionerRole role = practitionerRoleRepository
                .findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, wardId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRACTITIONER_ROLE_NOT_FOUND));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));

        return new AppProfileResponse(
                practitioner.getPractitionerId(),
                practitioner.getName(),
                practitioner.getEmployeeNumber(),
                role.getRoleCode().name(),
                role.getWard().getWardId(),
                role.getWard().getWardName(),
                organization.getOrganizationId(),
                organization.getName()
        );
    }
}
