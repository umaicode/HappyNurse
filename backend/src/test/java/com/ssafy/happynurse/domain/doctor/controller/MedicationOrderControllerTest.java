package com.ssafy.happynurse.domain.doctor.controller;

import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderItemResponse;
import com.ssafy.happynurse.domain.doctor.dto.MedicationOrderListResponse;
import com.ssafy.happynurse.domain.doctor.entity.OrderStatus;
import com.ssafy.happynurse.domain.doctor.entity.OrderType;
import com.ssafy.happynurse.domain.doctor.service.MedicationOrderService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MedicationOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class MedicationOrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    MedicationOrderService medicationOrderService;

    @BeforeEach
    void setUpSecurity() {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "EMP-001", "김간호", "nurse", "session-1", 1L, 3L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /patient/{patientId}/orders?date= - 조회 성공")
    void getOrders_성공() throws Exception {
        MedicationOrderItemResponse item = new MedicationOrderItemResponse(
                9L, OrderType.FLUID, "IV5001", "5% Dextrose Inj. 1L",
                new BigDecimal("1000"), 1, "bag", "IV", "60cc/hr 유지.",
                OrderStatus.active, LocalDateTime.of(2026, 4, 27, 14, 0),
                6L, "이조은");
        MedicationOrderListResponse response = new MedicationOrderListResponse(
                3L, "이승연", 1, List.of(item));

        given(medicationOrderService.getOrdersByPatientId(eq(3L), eq(LocalDate.of(2026, 4, 27))))
                .willReturn(response);

        mockMvc.perform(get("/patient/3/orders").param("date", "2026-04-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientName").value("이승연"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.orders[0].orderType").value("FLUID"))
                .andExpect(jsonPath("$.data.orders[0].prescriberName").value("이조은"));
    }

    @Test
    @DisplayName("GET /patient/{patientId}/orders?date= - 환자 없으면 404")
    void getOrders_실패_환자_없음() throws Exception {
        given(medicationOrderService.getOrdersByPatientId(eq(99L), any()))
                .willThrow(new CustomException(ErrorCode.PATIENT_NOT_FOUND));

        mockMvc.perform(get("/patient/99/orders").param("date", "2026-04-27"))
                .andExpect(status().isNotFound());
    }
}