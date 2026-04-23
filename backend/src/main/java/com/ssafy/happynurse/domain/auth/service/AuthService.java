package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import com.ssafy.happynurse.domain.auth.repository.SessionLogRepository;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final SessionLogRepository sessionLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult login(String employeeNumber, String password, String ipAddress,
                            Long organizationId, Long wardId) {
        Practitioner practitioner = practitionerRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, practitioner.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        PractitionerRole activeRole = practitionerRoleRepository
                .findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, wardId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "해당 병동에 대한 권한이 없습니다."));

        String roleCode = activeRole.getRoleCode().name();

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

        return new AuthResult(token, loginResponse);
    }

    @Transactional
    public void logout(String sessionId) {
        SessionLog sessionLog = sessionLogRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        sessionLog.markLogout();
    }
}
