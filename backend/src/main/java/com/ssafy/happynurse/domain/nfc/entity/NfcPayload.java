package com.ssafy.happynurse.domain.nfc.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnore
    public boolean isOrder() {
        return TYPE_ORDER.equals(type);
    }

    @JsonIgnore
    public boolean isDrug() {
        return TYPE_DRUG.equals(type);
    }
}
