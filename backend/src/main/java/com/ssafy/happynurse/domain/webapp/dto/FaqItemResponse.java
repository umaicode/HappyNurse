package com.ssafy.happynurse.domain.webapp.dto;

import com.ssafy.happynurse.domain.webapp.entity.Faq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "FAQ 단일 항목")
@Getter
@AllArgsConstructor
public class FaqItemResponse {

    @Schema(description = "인텐트 한글 라벨", example = "재활")
    private String intentLabel;

    @Schema(description = "표시 질문", example = "재활은 어떻게 진행되나요?")
    private String question;

    @Schema(description = "답변", example = "재활 운동은 ...")
    private String answer;

    public static FaqItemResponse from(Faq faq) {
        return new FaqItemResponse(
                faq.getIntent().getLabel(),
                faq.getIntent().getDisplayQuestion(),
                faq.getAnswer()
        );
    }
}