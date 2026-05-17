package com.ssafy.happynurse.domain.nurse.notification.controller;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import com.ssafy.happynurse.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseWardControllerTest {

    @Mock WardEmitterRegistry wardEmitterRegistry;

    @InjectMocks
    SseWardController controller;

    @Test
    void subscribe_registersWithWardIdFromJwt() throws IOException {
        CustomUserDetails user = new CustomUserDetails(
                10L, "N0010", "이승연", "NURSE", "session-1", 1L, 3L);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(wardEmitterRegistry.register(3L)).thenReturn(mockEmitter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SseEmitter result = controller.subscribe(user, response);

        assertThat(result).isSameAs(mockEmitter);
        verify(wardEmitterRegistry).register(3L);
        assertThat(response.getHeader("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
    }

    @Test
    void subscribe_nullWardId_throwsForbidden() {
        CustomUserDetails user = new CustomUserDetails(
                10L, "N0010", "이승연", "NURSE", "session-1", 1L, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> controller.subscribe(user, response))
                .isInstanceOf(CustomException.class);
    }
}