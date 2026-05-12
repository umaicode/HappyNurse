package com.ssafy.happynurse.domain.handover.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = """
        체크리스트 토글 요청.
        키 = "{slot_key}.{item_index}" (현재 synthesis.* 만 허용),
        값 = true (체크 추가), false (체크 해제 — 키 제거)
        한 번에 여러 키를 보낼 수 있음
        """,
        example = "{\"synthesis.0\": true, \"synthesis.2\": false}")
public record HandoverChecksPatchRequest(

        @NotNull
        @NotEmpty(message = "checks 가 비어있습니다.")
        Map<String, Boolean> checks
) {
}
