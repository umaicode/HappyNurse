package com.ssafy.happynurse.domain.nfc.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "nfc_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NfcTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nfc_tag_id")
    private Long nfcTagId;

    @Column(name = "tag_uid", nullable = false, length = 64)
    private String tagUid; // 물리적 NFC UID

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TagType tagType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
    private NfcPayload payloadJson; // {"type": "ORDER"|"DRUG", "id": <pk>}

    @Column(name = "is_active", nullable = false)
    private Boolean isActive; // 손상 시 FALSE

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static NfcTag issue(String tagUid, TagType tagType, NfcPayload payload, LocalDateTime issuedAt) {
        NfcTag t = new NfcTag();
        t.tagUid = tagUid;
        t.tagType = tagType;
        t.payloadJson = payload;
        t.isActive = true;
        t.issuedAt = issuedAt;
        return t;
    }

    /** 동일 시리얼 NFC 칩 재사용(re-issuance): 의미를 갈아 끼우고 활성 상태로 되돌린다. */
    public void reissue(TagType tagType, NfcPayload payload, LocalDateTime issuedAt) {
        this.tagType = tagType;
        this.payloadJson = payload;
        this.isActive = true;
        this.revokedAt = null;
        this.issuedAt = issuedAt;
    }
}
