package com.ssafy.happynurse.domain.nurse.notification.service;

import com.ssafy.happynurse.domain.nurse.notification.service.registry.PersonalEmitterRegistry;
import com.ssafy.happynurse.domain.nurse.notification.service.registry.WardEmitterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmitterHeartbeatSchedulerTest {

    @Mock
    WardEmitterRegistry wardEmitterRegistry;

    @Mock
    PersonalEmitterRegistry personalEmitterRegistry;

    @InjectMocks
    EmitterHeartbeatScheduler scheduler;

    @Test
    void heartbeat_callsBothRegistries() {
        scheduler.heartbeat();

        verify(wardEmitterRegistry).heartbeat();
        verify(personalEmitterRegistry).heartbeat();
    }
}