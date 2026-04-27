package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.entity.RefreshToken;
import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import com.ssafy.happynurse.domain.auth.repository.redis.RefreshTokenRepository;
import com.ssafy.happynurse.domain.auth.repository.SessionLogRepository;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.entity.RoleCode;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import com.ssafy.happynurse.domain.patient.repository.OrganizationRepository;
import com.ssafy.happynurse.domain.patient.repository.WardRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock PractitionerRepository practitionerRepository;
    @Mock PractitionerRoleRepository practitionerRoleRepository;
    @Mock SessionLogRepository sessionLogRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RefreshTokenReuseDetector reuseDetector;
    @Mock OrganizationRepository organizationRepository;
    @Mock WardRepository wardRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthService authService;

    // ──── 로그인 성공 ────

    @Test
    @DisplayName("로그인 성공 시 AuthResult를 반환한다")
    void login_성공() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        PractitionerRole role = createPractitionerRole(practitioner, RoleCode.nurse);
        Ward ward = createWard(3L);

        given(organizationRepository.existsById(1L)).willReturn(true);
        given(wardRepository.existsById(3L)).willReturn(true);
        given(wardRepository.findByWardIdAndOrganization_OrganizationId(3L, 1L)).willReturn(Optional.of(ward));
        given(practitionerRepository.findByEmployeeNumber("EMP001")).willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw")).willReturn(true);
        given(practitionerRoleRepository.findByPractitionerAndPeriodEndIsNull(practitioner)).willReturn(List.of(role));
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 3L)).willReturn(Optional.of(role));
        given(sessionLogRepository.save(any(SessionLog.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong())).willReturn("mock-jwt-token");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.login("EMP001", "password", "127.0.0.1", 1L, 3L, 604800000L);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("mock-jwt-token");
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.loginResponse().practitionerId()).isEqualTo(1L);
        assertThat(result.loginResponse().name()).isEqualTo("홍길동");
        assertThat(result.loginResponse().roleCode()).isEqualTo("nurse");
        assertThat(result.loginResponse().organizationId()).isEqualTo(1L);
        assertThat(result.loginResponse().wardId()).isEqualTo(3L);
        verify(sessionLogRepository).save(any(SessionLog.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ──── 기관/병동 검증 실패 ────

    @Test
    @DisplayName("존재하지 않는 기관 → ORGANIZATION_NOT_FOUND")
    void login_실패_존재하지_않는_기관() {
        given(organizationRepository.existsById(99L)).willReturn(false);

        assertLoginThrows("EMP001", "password", 99L, 3L, ErrorCode.ORGANIZATION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 병동 → WARD_NOT_FOUND")
    void login_실패_존재하지_않는_병동() {
        given(organizationRepository.existsById(1L)).willReturn(true);
        given(wardRepository.existsById(99L)).willReturn(false);

        assertLoginThrows("EMP001", "password", 1L, 99L, ErrorCode.WARD_NOT_FOUND);
    }

    @Test
    @DisplayName("기관에 속하지 않는 병동 → WARD_NOT_IN_ORGANIZATION")
    void login_실패_기관에_속하지_않는_병동() {
        given(organizationRepository.existsById(1L)).willReturn(true);
        given(wardRepository.existsById(3L)).willReturn(true);
        given(wardRepository.findByWardIdAndOrganization_OrganizationId(3L, 1L)).willReturn(Optional.empty());

        assertLoginThrows("EMP001", "password", 1L, 3L, ErrorCode.WARD_NOT_IN_ORGANIZATION);
    }

    // ──── 인증 실패 ────

    @Test
    @DisplayName("존재하지 않는 사원번호 → INVALID_CREDENTIALS")
    void login_실패_존재하지_않는_사원번호() {
        stubOrgAndWard();
        given(practitionerRepository.findByEmployeeNumber("WRONG")).willReturn(Optional.empty());

        assertLoginThrows("WRONG", "password", 1L, 3L, ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("잘못된 비밀번호 → INVALID_CREDENTIALS")
    void login_실패_잘못된_비밀번호() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        stubOrgAndWard();
        given(practitionerRepository.findByEmployeeNumber("EMP001")).willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("wrongPw", "hashedPw")).willReturn(false);

        assertLoginThrows("EMP001", "wrongPw", 1L, 3L, ErrorCode.INVALID_CREDENTIALS);
    }

    // ──── 권한 실패 ────

    @Test
    @DisplayName("퇴사한 직원(활성 역할 없음) → ACCOUNT_DISABLED")
    void login_실패_퇴사한_직원() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        stubOrgAndWard();
        given(practitionerRepository.findByEmployeeNumber("EMP001")).willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw")).willReturn(true);
        given(practitionerRoleRepository.findByPractitionerAndPeriodEndIsNull(practitioner)).willReturn(Collections.emptyList());

        assertLoginThrows("EMP001", "password", 1L, 3L, ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("해당 병동 권한 없음 → ROLE_NOT_FOUND")
    void login_실패_병동_권한없음() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        PractitionerRole otherRole = createPractitionerRole(practitioner, RoleCode.nurse);
        stubOrgAndWard();
        given(practitionerRepository.findByEmployeeNumber("EMP001")).willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw")).willReturn(true);
        given(practitionerRoleRepository.findByPractitionerAndPeriodEndIsNull(practitioner)).willReturn(List.of(otherRole));
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 3L)).willReturn(Optional.empty());

        assertLoginThrows("EMP001", "password", 1L, 3L, ErrorCode.ROLE_NOT_FOUND);
    }

    // ──── 리프레시 토큰 ────

    @Test
    @DisplayName("refresh 성공 시 새로운 토큰 쌍을 반환한다")
    void refresh_성공() {
        RefreshToken refreshToken = RefreshToken.create(
                "session-1", 1L, "EMP001", "홍길동",
                604800000L, 1L, 3L, "nurse");

        given(reuseDetector.getReusedSessionId(refreshToken.getTokenValue())).willReturn(null);
        given(refreshTokenRepository.findById(refreshToken.getTokenValue())).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.createAccessToken(1L, "EMP001", "홍길동", "nurse", "session-1", 1L, 3L)).willReturn("new-access-token");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.refresh(refreshToken.getTokenValue());

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.loginResponse().practitionerId()).isEqualTo(1L);
        verify(refreshTokenRepository).delete(refreshToken);
        verify(reuseDetector).markAsRotated(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("refresh 실패 - 존재하지 않는 토큰 → REFRESH_TOKEN_INVALID")
    void refresh_실패_존재하지_않는_토큰() {
        given(reuseDetector.getReusedSessionId("invalid-token")).willReturn(null);
        given(refreshTokenRepository.findById("invalid-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("invalid-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refresh 실패 - 재사용 감지 → REFRESH_TOKEN_REUSE_DETECTED")
    void refresh_실패_재사용_감지() {
        RefreshToken sessionToken = RefreshToken.create(
                "session-1", 1L, "EMP001", "홍길동",
                604800000L, 1L, 3L, "nurse");

        given(reuseDetector.getReusedSessionId("stolen-token")).willReturn("session-1");
        given(refreshTokenRepository.findBySessionId("session-1")).willReturn(List.of(sessionToken));

        assertThatThrownBy(() -> authService.refresh("stolen-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED));

        verify(refreshTokenRepository).deleteAll(List.of(sessionToken));
    }

    // ──── 로그아웃 ────

    @Test
    @DisplayName("로그아웃 시 해당 세션의 리프레시 토큰이 모두 삭제된다")
    void logout_성공_리프레시_토큰_삭제() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        SessionLog sessionLog = SessionLog.create(practitioner, "127.0.0.1");
        RefreshToken sessionToken = RefreshToken.create(
                sessionLog.getSessionId(), 1L, "EMP001", "홍길동",
                604800000L, 1L, 3L, "nurse");

        given(sessionLogRepository.findById(sessionLog.getSessionId())).willReturn(Optional.of(sessionLog));
        given(refreshTokenRepository.findBySessionId(sessionLog.getSessionId())).willReturn(List.of(sessionToken));

        authService.logout(sessionLog.getSessionId());

        verify(refreshTokenRepository).deleteAll(List.of(sessionToken));
    }

    @Test
    @DisplayName("로그아웃 성공 시 SessionLog의 logoutAt이 설정된다")
    void logout_성공() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        SessionLog sessionLog = SessionLog.create(practitioner, "127.0.0.1");
        given(sessionLogRepository.findById(sessionLog.getSessionId())).willReturn(Optional.of(sessionLog));
        given(refreshTokenRepository.findBySessionId(sessionLog.getSessionId())).willReturn(Collections.emptyList());

        authService.logout(sessionLog.getSessionId());

        assertThat(sessionLog.getLogoutAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 세션 로그아웃 → RESOURCE_NOT_FOUND")
    void logout_실패_세션없음() {
        given(sessionLogRepository.findById("non-existent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("non-existent"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // ──── 헬퍼 ────

    private void stubOrgAndWard() {
        Ward ward = createWard(3L);
        given(organizationRepository.existsById(1L)).willReturn(true);
        given(wardRepository.existsById(3L)).willReturn(true);
        given(wardRepository.findByWardIdAndOrganization_OrganizationId(3L, 1L)).willReturn(Optional.of(ward));
    }

    private void assertLoginThrows(String empNo, String password, Long orgId, Long wardId, ErrorCode expected) {
        assertThatThrownBy(() -> authService.login(empNo, password, "127.0.0.1", orgId, wardId, 604800000L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(expected));
    }

    private Practitioner createPractitioner(Long id, String empNo, String passwordHash, String name) {
        try {
            var constructor = Practitioner.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Practitioner p = constructor.newInstance();
            setField(p, "practitionerId", id);
            setField(p, "employeeNumber", empNo);
            setField(p, "passwordHash", passwordHash);
            setField(p, "name", name);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PractitionerRole createPractitionerRole(Practitioner practitioner, RoleCode roleCode) {
        try {
            var constructor = PractitionerRole.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PractitionerRole role = constructor.newInstance();
            setField(role, "practitioner", practitioner);
            setField(role, "roleCode", roleCode);
            return role;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Ward createWard(Long wardId) {
        try {
            var constructor = Ward.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Ward ward = constructor.newInstance();
            setField(ward, "wardId", wardId);
            return ward;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}