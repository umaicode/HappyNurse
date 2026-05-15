package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.service.NursingRecordSseService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiNursingRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiNursingRecordControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NursingRecordSseService nursingRecordSseService;

    @Test
    @DisplayName("notify 호출 성공 시 200 반환")
    void notify_returns200_whenSseServiceSucceeds() throws Exception {
        willDoNothing().given(nursingRecordSseService).send(42L);

        mockMvc.perform(post("/internal/ai/nursing-records/42/notify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 recordId면 404 반환")
    void notify_returns404_whenRecordNotFound() throws Exception {
        doThrow(new CustomException(ErrorCode.NURSING_RECORD_NOT_FOUND))
                .when(nursingRecordSseService).send(99L);

        mockMvc.perform(post("/internal/ai/nursing-records/99/notify"))
                .andExpect(status().isNotFound());
    }
}