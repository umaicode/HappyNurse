package com.ssafy.happynurse.domain.nfc.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record NfcPayload(
        String type,
        Long id
) {
    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_DRUG = "DRUG";

    @JsonCreator
    public NfcPayload(
            @JsonProperty("type") String type,
            @JsonProperty("id") Long id
    ) {
        this.type = type;
        this.id = id;
    }

    public boolean isOrder() {
        return TYPE_ORDER.equals(type);
    }

    public boolean isDrug() {
        return TYPE_DRUG.equals(type);
    }
}
