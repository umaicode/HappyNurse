package com.ssafy.happynurse.domain.patient.service;

import com.ssafy.happynurse.domain.patient.dto.OrganizationListResponse;
import com.ssafy.happynurse.domain.patient.dto.WardListResponse;
import com.ssafy.happynurse.domain.patient.entity.Organization;
import com.ssafy.happynurse.domain.patient.entity.Ward;
import com.ssafy.happynurse.domain.patient.repository.OrganizationRepository;
import com.ssafy.happynurse.domain.patient.repository.WardRepository;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock WardRepository wardRepository;
    @InjectMocks OrganizationService organizationService;

    @Test
    @DisplayName("listActiveOrganizations - 활성 기관만 정렬 순서대로 DTO로 변환되어 반환된다")
    void listActiveOrganizations_정상_활성_기관만_반환() {
        Organization org1 = createOrganization(1L, "가톨릭병원", "hospital", true);
        Organization org2 = createOrganization(2L, "싸피병원", "hospital", true);
        given(organizationRepository.findAllByActiveTrueOrderByNameAscOrganizationIdAsc())
                .willReturn(List.of(org1, org2));

        List<OrganizationListResponse> result = organizationService.listActiveOrganizations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).organizationId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("가톨릭병원");
        assertThat(result.get(0).typeCode()).isEqualTo("hospital");
        assertThat(result.get(1).organizationId()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("싸피병원");
    }

    @Test
    @DisplayName("listActiveOrganizations - 활성 기관이 없으면 빈 리스트")
    void listActiveOrganizations_빈_결과() {
        given(organizationRepository.findAllByActiveTrueOrderByNameAscOrganizationIdAsc())
                .willReturn(List.of());

        List<OrganizationListResponse> result = organizationService.listActiveOrganizations();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listWardsByOrganization - 활성 기관의 병동 목록을 정렬 순서대로 DTO로 반환한다")
    void listWardsByOrganization_정상_정렬된_병동목록_반환() {
        Organization org = createOrganization(1L, "싸피병원", "hospital", true);
        Ward w1 = createWard(10L, "내과 1병동");
        Ward w2 = createWard(20L, "외과 2병동");
        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
        given(wardRepository.findAllByOrganization_OrganizationIdOrderByWardNameAscWardIdAsc(1L))
                .willReturn(List.of(w1, w2));

        List<WardListResponse> result = organizationService.listWardsByOrganization(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).wardId()).isEqualTo(10L);
        assertThat(result.get(0).wardName()).isEqualTo("내과 1병동");
        assertThat(result.get(1).wardId()).isEqualTo(20L);
        assertThat(result.get(1).wardName()).isEqualTo("외과 2병동");
    }

    @Test
    @DisplayName("listWardsByOrganization - 활성 기관이지만 병동이 0건이면 빈 리스트")
    void listWardsByOrganization_정상_빈_결과() {
        Organization org = createOrganization(1L, "싸피병원", "hospital", true);
        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
        given(wardRepository.findAllByOrganization_OrganizationIdOrderByWardNameAscWardIdAsc(1L))
                .willReturn(List.of());

        List<WardListResponse> result = organizationService.listWardsByOrganization(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listWardsByOrganization - 존재하지 않는 기관이면 ORGANIZATION_NOT_FOUND")
    void listWardsByOrganization_존재하지_않는_기관이면_ORGANIZATION_NOT_FOUND() {
        given(organizationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.listWardsByOrganization(99L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORGANIZATION_NOT_FOUND));
    }

    @Test
    @DisplayName("listWardsByOrganization - 비활성 기관도 동일하게 ORGANIZATION_NOT_FOUND")
    void listWardsByOrganization_비활성_기관이면_ORGANIZATION_NOT_FOUND() {
        Organization inactive = createOrganization(1L, "폐업병원", "hospital", false);
        given(organizationRepository.findById(1L)).willReturn(Optional.of(inactive));

        assertThatThrownBy(() -> organizationService.listWardsByOrganization(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORGANIZATION_NOT_FOUND));
    }

    // ──── 헬퍼 ────

    private Organization createOrganization(Long id, String name, String typeCode, boolean active) {
        try {
            var constructor = Organization.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Organization o = constructor.newInstance();
            setField(o, "organizationId", id);
            setField(o, "name", name);
            setField(o, "typeCode", typeCode);
            setField(o, "active", active);
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Ward createWard(Long id, String wardName) {
        try {
            var constructor = Ward.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Ward w = constructor.newInstance();
            setField(w, "wardId", id);
            setField(w, "wardName", wardName);
            return w;
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
