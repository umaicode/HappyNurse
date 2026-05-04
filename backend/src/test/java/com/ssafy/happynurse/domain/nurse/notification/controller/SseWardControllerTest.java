package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import com.ssafy.happynurse.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseWardControllerTest {

    @Mock WardEmitterRegistry wardEmitterRegistry;

    @InjectMocks
    SseWardController controller;

    @Test
    void subscribe_registersWithWardIdFromJwt() {
        CustomUserDetails user = new CustomUserDetails(
                10L, "N0010", "이승연", "NURSE", "session-1", 1L, 3L);
        SseEmitter mockEmitter = new SseEmitter();
        when(wardEmitterRegistry.register(3L)).thenReturn(mockEmitter);

        SseEmitter result = controller.subscribe(user);

        assertThat(result).isSameAs(mockEmitter);
        verify(wardEmitterRegistry).register(3L);
    }

    @Test
    void subscribe_nullWardId_throwsForbidden() {
        CustomUserDetails user = new CustomUserDetails(
                10L, "N0010", "이승연", "NURSE", "session-1", 1L, null);

        assertThatThrownBy(() -> controller.subscribe(user))
                .isInstanceOf(CustomException.class);
    }
}