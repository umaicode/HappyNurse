package com.ssafy.happynurse.domain.handover.dto;

import com.ssafy.happynurse.domain.handover.entity.ShiftHandover;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record HandoverDetailResponse(

        @Schema(description = "인수인계 PK", example = "42")
        String handoverId,

        @Schema(description = "체크리스트 상태. key=\"{slot_key}.{item_index}\" (현재 synthesis.* 만 사용), value={by,at}",
                example = "{\"synthesis.0\":{\"by\":7,\"at\":\"2026-05-12T08:13:00\"}}")
        Map<String, Object> checkedItemsJson
) {
    public static HandoverDetailResponse from(ShiftHandover h) {
        return new HandoverDetailResponse(
                String.valueOf(h.getHandoverId()),
                h.getCheckedItemsJson() == null ? Map.of() : h.getCheckedItemsJson()
        );
    }
}
