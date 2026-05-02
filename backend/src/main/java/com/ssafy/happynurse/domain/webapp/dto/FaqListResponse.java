package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "환자 FAQ 응답")
@Getter
@AllArgsConstructor
public class FaqListResponse {

    @Schema(description = "환자 Encounter의 원본 병명", example = "퇴행성 무릎 관절염")
    private String diseaseName;

    @Schema(description = "매칭된 FAQ 질환명 (없으면 null)", example = "퇴행성 관절염")
    private String matchedFaqDisease;

    @Schema(description = "정렬된 FAQ 리스트")
    private List<FaqItemResponse> items;
}