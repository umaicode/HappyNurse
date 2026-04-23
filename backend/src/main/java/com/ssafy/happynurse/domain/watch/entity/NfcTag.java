package com.ssafy.happynurse.domain.watch.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "encrypted_payload", nullable = false)
    private byte[] encryptedPayload; // 암호화된 페이로드

    @Column(name = "encryption_key_id", nullable = false, length = 64)
    private String encryptionKeyId; // KMS 키 참조 ID

    @Column(name = "is_active", nullable = false)
    private Boolean isActive; // 손상 시 FALSE

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}