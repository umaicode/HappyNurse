package com.ssafy.happynurse.domain.device.dto;

import com.ssafy.happynurse.domain.common.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequest(
        @NotBlank @Size(min = 1, max = 512) String fcmToken,
        @NotNull DeviceType deviceType
) {}