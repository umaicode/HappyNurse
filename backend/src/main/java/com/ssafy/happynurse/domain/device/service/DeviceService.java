package com.ssafy.happynurse.domain.device.service;

import com.ssafy.happynurse.domain.common.entity.PractitionerDevice;
import com.ssafy.happynurse.domain.common.entity.PractitionerRole;
import com.ssafy.happynurse.domain.common.entity.DeviceType;
import com.ssafy.happynurse.domain.common.repository.PractitionerDeviceRepository;
import com.ssafy.happynurse.domain.common.repository.PractitionerRoleRepository;
import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterRequest;
import com.ssafy.happynurse.domain.device.dto.FcmTokenRegisterResponse;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final PractitionerRoleRepository roleRepository;
    private final PractitionerDeviceRepository deviceRepository;

    public FcmTokenRegisterResponse registerFcmToken(
            Long practitionerId, Long wardId, FcmTokenRegisterRequest request) {

        DeviceType type = request.deviceType();
        if (type != DeviceType.mobile && type != DeviceType.watch) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "FCM 등록은 mobile/watch deviceType 만 허용 (요청 값: " + type + ")");
        }

        PractitionerRole role = roleRepository
                .findActiveByPractitionerIdAndWardId(practitionerId, wardId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "활성 PractitionerRole 없음 (practitionerId=" + practitionerId
                                + ", wardId=" + wardId + ")"));

        PractitionerDevice device = deviceRepository
                .findByPractitionerRole_PractitionerRoleIdAndFcmToken(
                        role.getPractitionerRoleId(), request.fcmToken())
                .map(existing -> {
                    existing.activate();
                    existing.touchLastUsed();
                    return existing;
                })
                .orElseGet(() -> deviceRepository.save(
                        PractitionerDevice.create(role, request.fcmToken(), request.deviceType())));

        return new FcmTokenRegisterResponse(device.getDeviceId());
    }
}