package com.ssafy.happynurse.domain.auth.entity;

import com.ssafy.happynurse.domain.common.entity.Practitioner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionLog {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    public static SessionLog create(Practitioner practitioner, String ipAddress) {
        SessionLog log = new SessionLog();
        log.sessionId = UUID.randomUUID().toString();
        log.practitioner = practitioner;
        log.ipAddress = ipAddress;
        log.loginAt = LocalDateTime.now();
        return log;
    }

    public void markLogout() {
        this.logoutAt = LocalDateTime.now();
    }
}