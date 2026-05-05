package com.ssafy.happynurse.domain.nfc.dto;

import com.ssafy.happynurse.domain.nfc.entity.NfcPayload;
import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.nfc.entity.TagType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NfcTagIssueResponse(
        @Schema(description = "NFC 태그 PK", example = "42")
        Long nfcTagId,

        @Schema(description = "발급된 시리얼", example = "04A1B2C3D4E5F6")
        String tagUid,

        @Schema(description = "태그 종류", example = "medication")
        TagType tagType,

        @Schema(description = "등록된 payload")
        NfcPayload payload,

        @Schema(description = "활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "발급 시각", example = "2026-05-04T14:00:00")
        LocalDateTime issuedAt
) {
    public static NfcTagIssueResponse from(NfcTag tag) {
        return new NfcTagIssueResponse(
                tag.getNfcTagId(),
                tag.getTagUid(),
                tag.getTagType(),
                tag.getPayloadJson(),
                tag.getIsActive(),
                tag.getIssuedAt()
        );
    }
}
