package com.ssafy.happynurse.domain.nfc.dto;

import com.ssafy.happynurse.domain.nfc.entity.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NfcTagIssueRequest(
        @Schema(description = "발급 대상 NFC 칩의 시리얼(공장 UID 또는 NDEF 작성 UID)", example = "04:69:C1:48:C6:2A:81")
        @NotBlank
        @Size(max = 64)
        String tagUid,

        @Schema(description = "태그 종류", example = "medication")
        @NotNull
        TagType tagType,

        @Schema(description = "payload type — medication 태그는 ORDER(수액/주사) 또는 DRUG(알약)", example = "ORDER")
        @NotBlank
        @Pattern(regexp = "ORDER|DRUG", message = "payloadType 은 ORDER 또는 DRUG 만 허용됩니다.")
        String payloadType,

        @Schema(description = "참조 PK — ORDER 면 medication_order_id, DRUG 면 medication_id", example = "9")
        @NotNull
        @Positive
        Long payloadId
) {
}
