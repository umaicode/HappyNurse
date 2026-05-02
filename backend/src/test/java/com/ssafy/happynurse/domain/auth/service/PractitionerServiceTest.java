package com.ssafy.happynurse.domain.auth.service;

import com.ssafy.happynurse.domain.auth.dto.PractitionerMeResponse;
import com.ssafy.happynurse.domain.common.entity.Practitioner;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.entity.RoleCode;
import com.ssafy.happynurse.domain.common.repository.PractitionerRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PractitionerServiceTest {

    @Mock PractitionerRepository practitionerRepository;
    @Mock PractitionerRoleRepository practitionerRoleRepository;
    @InjectMocks PractitionerService practitionerService;

    @Test
    @DisplayName("getMyInfo 성공 시 Practitioner 기본 정보와 활성 역할 정보를 반환한다")
    void getMyInfo_성공() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "홍길동");
        Ward ward = createWard(3L, "내과 3병동");
        PractitionerRole role = createPractitionerRole(practitioner, ward, RoleCode.nurse);

        given(practitionerRepository.findById(1L)).willReturn(Optional.of(practitioner));
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 3L))
                .willReturn(Optional.of(role));

        PractitionerMeResponse response = practitionerService.getMyInfo(1L, 3L);

        assertThat(response).isNotNull();
        assertThat(response.practitionerId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.employeeNumber()).isEqualTo("EMP001");
        assertThat(response.roleCode()).isEqualTo("nurse");
        assertThat(response.wardId()).isEqualTo(3L);
        assertThat(response.wardName()).isEqualTo("내과 3병동");
    }

    @Test
    @DisplayName("Practitioner가 존재하지 않으면 PRACTITIONER_NOT_FOUND")
    void getMyInfo_실패_Practitioner_없음() {
        given(practitionerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> practitionerService.getMyInfo(99L, 3L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTITIONER_NOT_FOUND));
    }

    @Test
    @DisplayName("해당 병동의 활성 역할이 없으면 PRACTITIONER_ROLE_NOT_FOUND")
    void getMyInfo_실패_활성_역할_없음() {
        Practitioner practitioner = createPractitioner(1L, "EMP001", "홍길동");

        given(practitionerRepository.findById(1L)).willReturn(Optional.of(practitioner));
        given(practitionerRoleRepository.findByPractitionerAndWard_WardIdAndPeriodEndIsNull(practitioner, 3L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> practitionerService.getMyInfo(1L, 3L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTITIONER_ROLE_NOT_FOUND));
    }

    // ──── 헬퍼 ────

    private Practitioner createPractitioner(Long id, String empNo, String name) {
        try {
            var constructor = Practitioner.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Practitioner p = constructor.newInstance();
            setField(p, "practitionerId", id);
            setField(p, "employeeNumber", empNo);
            setField(p, "name", name);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Ward createWard(Long wardId, String wardName) {
        try {
            var constructor = Ward.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Ward ward = constructor.newInstance();
            setField(ward, "wardId", wardId);
            setField(ward, "wardName", wardName);
            return ward;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PractitionerRole createPractitionerRole(Practitioner practitioner, Ward ward, RoleCode roleCode) {
        try {
            var constructor = PractitionerRole.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PractitionerRole role = constructor.newInstance();
            setField(role, "practitioner", practitioner);
            setField(role, "ward", ward);
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