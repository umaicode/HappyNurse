package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginResponse;
import com.ssafy.happynurse.domain.auth.entity.RefreshToken;
import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import com.ssafy.happynurse.domain.auth.repository.redis.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenReuseDetector reuseDetector;
    private final OrganizationRepository organizationRepository;
    private final WardRepository wardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult login(String employeeNumber, String password, String ipAddress,
                            Long organizationId, Long wardId, long refreshExpirationMs) {
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

        RefreshToken refreshToken = RefreshToken.create(
                sessionLog.getSessionId(),
                practitioner.getPractitionerId(),
                practitioner.getEmployeeNumber(),
                practitioner.getName(),
                refreshExpirationMs,
                organizationId, wardId, roleCode);
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(token, refreshToken.getTokenValue(), loginResponse);
    }

    public AuthResult refresh(String refreshTokenValue) {
        // 1. 재사용 탐지
        String reusedSessionId = reuseDetector.getReusedSessionId(refreshTokenValue);
        if (reusedSessionId != null) {
            List<RefreshToken> sessionTokens = refreshTokenRepository.findBySessionId(reusedSessionId);
            refreshTokenRepository.deleteAll(sessionTokens);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        // 2. 토큰 조회 (없으면 만료되었거나 유효하지 않음)
        RefreshToken refreshToken = refreshTokenRepository.findById(refreshTokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));

        // 3. 기존 토큰 삭제 + 재사용 마커 등록
        refreshTokenRepository.delete(refreshToken);
        reuseDetector.markAsRotated(refreshTokenValue, refreshToken.getSessionId(),
                jwtTokenProvider.getRefreshTokenExpirationMs());

        // 4. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                refreshToken.getPractitionerId(),
                refreshToken.getEmployeeNumber(),
                refreshToken.getPractitionerName(),
                refreshToken.getRoleCode(),
                refreshToken.getSessionId(),
                refreshToken.getOrganizationId(),
                refreshToken.getWardId()
        );

        RefreshToken newRefreshToken = RefreshToken.create(
                refreshToken.getSessionId(),
                refreshToken.getPractitionerId(),
                refreshToken.getEmployeeNumber(),
                refreshToken.getPractitionerName(),
                refreshToken.getTtl(),
                refreshToken.getOrganizationId(),
                refreshToken.getWardId(),
                refreshToken.getRoleCode());
        refreshTokenRepository.save(newRefreshToken);

        LoginResponse loginResponse = new LoginResponse(
                refreshToken.getPractitionerId(),
                refreshToken.getPractitionerName(),
                refreshToken.getEmployeeNumber(),
                refreshToken.getRoleCode(),
                refreshToken.getOrganizationId(),
                refreshToken.getWardId()
        );

        return new AuthResult(newAccessToken, newRefreshToken.getTokenValue(), loginResponse);
    }

    @Transactional
    public void logout(String sessionId) {
        SessionLog sessionLog = sessionLogRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        sessionLog.markLogout();

        List<RefreshToken> sessionTokens = refreshTokenRepository.findBySessionId(sessionId);
        refreshTokenRepository.deleteAll(sessionTokens);
    }
}