package com.ssafy.happynurse.domain.webapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SymptomTranscribeResponse(
        @Schema(description = "STT 변환 텍스트 (raw, 의료 용어 자동 교정 미적용)",
                example = "허리가 너무 아파요")
        String text
) {}
