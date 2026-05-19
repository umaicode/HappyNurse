package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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

    // AI filter Pydantic max_length=2000 과 일치. 더 길면 정제 우회되어 욕설 노출 위험 + DB 폭발 방지.
    @Schema(description = "직접 입력 증상 텍스트 (직접 입력 시, 최대 2000자)", example = "열이 납니다", maxLength = 2000)
    @Size(max = 2000, message = "증상 텍스트는 2000자를 초과할 수 없습니다.")
    private String symptomText;
}
