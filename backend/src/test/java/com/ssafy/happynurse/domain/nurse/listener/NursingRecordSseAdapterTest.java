package com.ssafy.happynurse.domain.nurse.listener;

import com.ssafy.happynurse.domain.nurse.event.NursingRecordSavedEvent;
import com.ssafy.happynurse.domain.nurse.service.NursingRecordSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NursingRecordSseAdapterTest {

    @Mock NursingRecordSseService nursingRecordSseService;
    @InjectMocks NursingRecordSseAdapter adapter;

    @Test
    @DisplayName("이벤트 수신 시 SseService.send()를 nursingRecordId로 호출한다")
    void on_delegatesToSseService() {
        NursingRecordSavedEvent event = new NursingRecordSavedEvent(42L, 10L, 100L);

        adapter.on(event);

        verify(nursingRecordSseService).send(42L);
    }
}