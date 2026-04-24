package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import com.ssafy.happynurse.domain.auth.repository.SessionLogRepository;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.patient.repository.OrganizationRepository;
import com.ssafy.happynurse.domain.patient.repository.WardRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final SessionLogRepository sessionLogRepository;
    private final OrganizationRepository organizationRepository;
    private final WardRepository wardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult login(String employeeNumber, String password, String ipAddress,
                            Long organizationId, Long wardId) {
        // 1. 기관 존재 확인
        if (!organizationRepository.existsById(organizationId)) {
            throw new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }

        // 2. 병동 존재 확인
        if (!wardRepository.existsById(wardId)) {
            throw new CustomException(ErrorCode.WARD_NOT_FOUND);
        }

        // 3. 병동이 해당 기관 소속인지 확인
        wardRepository.findByWardIdAndOrganization_OrganizationId(wardId, organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.WARD_NOT_IN_ORGANIZATION));

        // 4. 사원번호 조회
        Practitioner practitioner = practitionerRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 5. 비밀번호 검증
        if (!passwordEncoder.matches(password, practitioner.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 6. 활성 역할 존재 확인 (퇴사 여부)
        List<PractitionerRole> activeRoles =
                practitionerRoleRepository.findByPractitionerAndPeriodEndIsNull(practitioner);
        if (activeRoles.isEmpty()) {
            throw new CustomException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 7. 해당 병동 역할 확인
        PractitionerRole wardRole = practitionerRoleRepository
                .findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, wardId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));

        String roleCode = wardRole.getRoleCode().name();

        // 8. 세션 생성 + JWT 발급
        SessionLog sessionLog = SessionLog.create(practitioner, ipAddress);
        sessionLogRepository.save(sessionLog);

        String token = jwtTokenProvider.createAccessToken(
                practitioner.getPractitionerId(),
                practitioner.getEmployeeNumber(),
                practitioner.getName(),
                roleCode,
                sessionLog.getSessionId(),
                organizationId,
                wardId
        );

        LoginResponse loginResponse = new LoginResponse(
                practitioner.getPractitionerId(),
                practitioner.getName(),
                practitioner.getEmployeeNumber(),
                roleCode,
                organizationId,
                wardId
        );

        return new AuthResult(token, null, loginResponse);
    }

    @Transactional
    public void logout(String sessionId) {
        SessionLog sessionLog = sessionLogRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        sessionLog.markLogout();
    }
}
