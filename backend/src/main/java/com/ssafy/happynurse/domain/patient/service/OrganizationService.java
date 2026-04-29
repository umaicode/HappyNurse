package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.patient.dto.OrganizationListResponse;
import com.ssafy.happynurse.domain.patient.dto.WardListResponse;
import com.ssafy.happynurse.domain.patient.entity.Organization;
import com.ssafy.happynurse.domain.patient.repository.OrganizationRepository;
import com.ssafy.happynurse.domain.patient.repository.WardRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final WardRepository wardRepository;

    public List<OrganizationListResponse> listActiveOrganizations() {
        return organizationRepository.findAllByActiveTrueOrderByNameAscOrganizationIdAsc().stream()
                .map(o -> new OrganizationListResponse(o.getOrganizationId(), o.getName(), o.getTypeCode()))
                .toList();
    }

    public List<WardListResponse> listWardsByOrganization(Long organizationId) {
        Optional<Organization> found = organizationRepository.findById(organizationId);
        if (found.isEmpty()) {
            log.info("organization not found: id={}", organizationId);
            throw new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(found.get().getActive())) {
            log.info("organization inactive: id={}", organizationId);
            throw new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }

        return wardRepository
                .findAllByOrganization_OrganizationIdOrderByWardNameAscWardIdAsc(organizationId).stream()
                .map(w -> new WardListResponse(w.getWardId(), w.getWardName()))
                .toList();
    }
}
