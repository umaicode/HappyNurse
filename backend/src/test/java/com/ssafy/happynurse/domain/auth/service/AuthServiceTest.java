package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.entity.SessionLog;
import com.ssafy.happynurse.domain.auth.repository.SessionLogRepository;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.entity.RoleCode;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
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

    @Mock
    PractitionerRepository practitionerRepository;
    @Mock
    PractitionerRoleRepository practitionerRoleRepository;
    @Mock
    SessionLogRepository sessionLogRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("로그인 성공 시 AuthResult를 반환한다")
    void login_성공() {
        // given
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        PractitionerRole role = createPractitionerRole(practitioner, RoleCode.nurse);

        given(practitionerRepository.findByEmployeeNumber("EMP001"))
                .willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw"))
                .willReturn(true);
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 3L))
                .willReturn(Optional.of(role));
        given(sessionLogRepository.save(any(SessionLog.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong()))
                .willReturn("mock-jwt-token");

        // when
        AuthResult result = authService.login("EMP001", "password", "127.0.0.1", 1L, 3L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("mock-jwt-token");
        assertThat(result.loginResponse().practitionerId()).isEqualTo(1L);
        assertThat(result.loginResponse().name()).isEqualTo("홍길동");
        assertThat(result.loginResponse().employeeNumber()).isEqualTo("EMP001");
        assertThat(result.loginResponse().roleCode()).isEqualTo("nurse");
        assertThat(result.loginResponse().organizationId()).isEqualTo(1L);
        assertThat(result.loginResponse().wardId()).isEqualTo(3L);
        verify(sessionLogRepository).save(any(SessionLog.class));
    }

    @Test
    @DisplayName("존재하지 않는 사원번호로 로그인 시 INVALID_CREDENTIALS 예외가 발생한다")
    void login_실패_존재하지_않는_사원번호() {
        // given
        given(practitionerRepository.findByEmployeeNumber("WRONG"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login("WRONG", "password", "127.0.0.1", 1L, 3L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 INVALID_CREDENTIALS 예외가 발생한다")
    void login_실패_잘못된_비밀번호() {
        // given
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        given(practitionerRepository.findByEmployeeNumber("EMP001"))
                .willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("wrongPw", "hashedPw"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login("EMP001", "wrongPw", "127.0.0.1", 1L, 3L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("해당 병동 권한이 없으면 FORBIDDEN 예외가 발생한다")
    void login_실패_병동_권한없음() {
        // given
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        given(practitionerRepository.findByEmployeeNumber("EMP001"))
                .willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw"))
                .willReturn(true);
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 99L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login("EMP001", "password", "127.0.0.1", 1L, 99L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("로그아웃 성공 시 SessionLog의 logoutAt이 설정된다")
    void logout_성공() {
        // given
        Practitioner practitioner = createPractitioner(1L, "EMP001", "hashedPw", "홍길동");
        SessionLog sessionLog = SessionLog.create(practitioner, "127.0.0.1");
        given(sessionLogRepository.findById(sessionLog.getSessionId()))
                .willReturn(Optional.of(sessionLog));

        // when
        authService.logout(sessionLog.getSessionId());

        // then
        assertThat(sessionLog.getLogoutAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 로그아웃 시 RESOURCE_NOT_FOUND 예외가 발생한다")
    void logout_실패_세션없음() {
        // given
        given(sessionLogRepository.findById("non-existent"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout("non-existent"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
