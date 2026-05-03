package com.ssafy.happynurse.domain.device.service;

import com.ssafy.happynurse.domain.common.entity.DeviceType;
import com.ssafy.happynurse.domain.common.entity.PractitionerDevice;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.repository.PractitionerDeviceRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterRequest;
import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterResponse;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock PractitionerRoleRepository roleRepository;
    @Mock PractitionerDeviceRepository deviceRepository;
    @InjectMocks DeviceService deviceService;

    @Test
    @DisplayName("신규 토큰이면 INSERT 후 deviceId 반환")
    void registersNewDevice() {
        // given
        Long practitionerId = 1L;
        Long wardId = 10L;
        PractitionerRole role = mockRole(100L);
        given(roleRepository.findActiveByPractitionerIdAndWardId(practitionerId, wardId))
                .willReturn(Optional.of(role));
        given(deviceRepository.findByPractitionerRole_PractitionerRoleIdAndFcmToken(100L, "tokenA"))
                .willReturn(Optional.empty());
        PractitionerDevice saved = PractitionerDevice.create(role, "tokenA", DeviceType.mobile);
        injectDeviceId(saved, 999L);
        given(deviceRepository.save(any(PractitionerDevice.class))).willReturn(saved);

        // when
        FcmTokenRegisterResponse response = deviceService.registerFcmToken(
                practitionerId, wardId,
                new FcmTokenRegisterRequest("tokenA", DeviceType.mobile));

        // then
        assertThat(response.deviceId()).isEqualTo(999L);
        ArgumentCaptor<PractitionerDevice> captor = ArgumentCaptor.forClass(PractitionerDevice.class);
        verify(deviceRepository).save(captor.capture());
        assertThat(captor.getValue().getFcmToken()).isEqualTo("tokenA");
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("동일 토큰이 이미 있으면 activate + touchLastUsed 후 기존 deviceId 반환")
    void upsertsExistingDevice() {
        PractitionerRole role = mockRole(100L);
        PractitionerDevice existing = PractitionerDevice.create(role, "tokenA", DeviceType.mobile);
        injectDeviceId(existing, 555L);
        existing.deactivate();  // 비활성화 상태에서 재등록 시뮬레이션

        given(roleRepository.findActiveByPractitionerIdAndWardId(1L, 10L)).willReturn(Optional.of(role));
        given(deviceRepository.findByPractitionerRole_PractitionerRoleIdAndFcmToken(100L, "tokenA"))
                .willReturn(Optional.of(existing));

        FcmTokenRegisterResponse response = deviceService.registerFcmToken(
                1L, 10L, new FcmTokenRegisterRequest("tokenA", DeviceType.mobile));

        assertThat(response.deviceId()).isEqualTo(555L);
        assertThat(existing.getIsActive()).isTrue();
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 role 없으면 CustomException(ROLE_NOT_FOUND)")
    void throwsWhenNoActiveRole() {
        given(roleRepository.findActiveByPractitionerIdAndWardId(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.registerFcmToken(
                1L, 10L, new FcmTokenRegisterRequest("tokenA", DeviceType.mobile)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("deviceType=web 이면 INVALID_INPUT_VALUE 거부")
    void rejectsNonMobileDeviceType() {
        assertThatThrownBy(() -> deviceService.registerFcmToken(
                1L, 10L, new FcmTokenRegisterRequest("tokenA", DeviceType.web)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(roleRepository, never()).findActiveByPractitionerIdAndWardId(anyLong(), anyLong());
        verify(deviceRepository, never()).save(any());
    }

    private PractitionerRole mockRole(Long id) {
        PractitionerRole role = org.mockito.Mockito.mock(PractitionerRole.class);
        given(role.getPractitionerRoleId()).willReturn(id);
        return role;
    }

    private void injectDeviceId(PractitionerDevice device, Long id) {
        try {
            var field = PractitionerDevice.class.getDeclaredField("deviceId");
            field.setAccessible(true);
            field.set(device, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}