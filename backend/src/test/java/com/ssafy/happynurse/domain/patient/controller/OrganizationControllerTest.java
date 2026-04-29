package com.ssafy.happynurse.domain.patient.controller;

import com.ssafy.happynurse.domain.patient.dto.OrganizationListResponse;
import com.ssafy.happynurse.domain.patient.dto.WardListResponse;
import com.ssafy.happynurse.domain.patient.service.OrganizationService;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;
import com.ssafy.happynurse.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrganizationController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrganizationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrganizationService organizationService;

    @Test
    @DisplayName("GET /organizations 성공 시 200 + 활성 병원 목록 반환")
    void listOrganizations_성공() throws Exception {
        given(organizationService.listActiveOrganizations()).willReturn(List.of(
                new OrganizationListResponse(1L, "가톨릭병원", "hospital"),
                new OrganizationListResponse(2L, "싸피병원", "hospital")
        ));

        mockMvc.perform(get("/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("병원 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data[0].organizationId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("가톨릭병원"))
                .andExpect(jsonPath("$.data[0].typeCode").value("hospital"))
                .andExpect(jsonPath("$.data[1].organizationId").value(2))
                .andExpect(jsonPath("$.data[1].name").value("싸피병원"));
    }

    @Test
    @DisplayName("GET /organizations/{id}/wards 성공 시 200 + 병동 목록 반환")
    void listWards_성공() throws Exception {
        given(organizationService.listWardsByOrganization(1L)).willReturn(List.of(
                new WardListResponse(10L, "내과 1병동"),
                new WardListResponse(20L, "외과 2병동")
        ));

        mockMvc.perform(get("/organizations/1/wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("병동 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data[0].wardId").value(10))
                .andExpect(jsonPath("$.data[0].wardName").value("내과 1병동"))
                .andExpect(jsonPath("$.data[1].wardId").value(20))
                .andExpect(jsonPath("$.data[1].wardName").value("외과 2병동"));
    }

    @Test
    @DisplayName("GET /organizations/{id}/wards - 존재하지 않거나 비활성 기관이면 404 + ORGANIZATION_NOT_FOUND")
    void listWards_실패_기관_없음() throws Exception {
        given(organizationService.listWardsByOrganization(99L))
                .willThrow(new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));

        mockMvc.perform(get("/organizations/99/wards"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ORGANIZATION_NOT_FOUND"));
    }
}