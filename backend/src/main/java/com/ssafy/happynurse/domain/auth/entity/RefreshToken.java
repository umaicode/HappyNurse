package com.ssafy.happynurse.domain.auth.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "token_value", nullable = false, unique = true, length = 64)
    private String tokenValue;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "role_code", nullable = false, length = 20)
    private String roleCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RefreshToken create(String sessionId, Practitioner practitioner,
                                       long expirationMs, Long organizationId,
                                       Long wardId, String roleCode) {
        RefreshToken token = new RefreshToken();
        token.tokenValue = UUID.randomUUID().toString();
        token.sessionId = sessionId;
        token.practitioner = practitioner;
        token.organizationId = organizationId;
        token.wardId = wardId;
        token.roleCode = roleCode;
        token.revoked = false;
        token.createdAt = LocalDateTime.now();
        token.expiresAt = token.createdAt.plusSeconds(expirationMs / 1000);
        return token;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isUsable() {
        return !revoked && LocalDateTime.now().isBefore(expiresAt);
    }
}
