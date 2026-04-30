package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "증상 제출 요청 (buttonId와 symptomText 중 하나 이상 입력. 둘 다 입력 시 '버튼라벨 - 추가텍스트' 형식으로 저장)")
@Getter
@Setter
@NoArgsConstructor
public class SymptomSubmitRequest {

    @Schema(description = "증상 버튼 ID (버튼 선택 시)", example = "1")
    private Long buttonId;

    @Schema(description = "직접 입력 증상 텍스트 (직접 입력 시)", example = "열이 납니다")
    private String symptomText;
}
