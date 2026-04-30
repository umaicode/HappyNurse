package com.ssafy.happynurse.domain.webapp.dto;

import com.ssafy.happynurse.domain.webapp.entity.QuickSymptomButton;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "증상 버튼 응답")
@Getter
@AllArgsConstructor
public class SymptomButtonResponse {

    @Schema(description = "버튼 ID", example = "1")
    private Long buttonId;

    @Schema(description = "버튼 라벨", example = "드레싱 교체")
    private String label;

    @Schema(description = "버튼 설명", example = "상처 부위 드레싱이 필요합니다")
    private String description;

    @Schema(description = "표시 순서", example = "1")
    private Integer displayOrder;

    public static SymptomButtonResponse from(QuickSymptomButton button) {
        return new SymptomButtonResponse(
                button.getButtonId(),
                button.getLabel(),
                button.getDescription(),
                button.getDisplayOrder()
        );
    }
}
