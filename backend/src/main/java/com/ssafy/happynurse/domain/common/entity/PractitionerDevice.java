package com.ssafy.happynurse.domain.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "practitioner_device")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PractitionerDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_role_id", nullable = false)
    private PractitionerRole practitionerRole;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public static PractitionerDevice create(PractitionerRole role, String fcmToken, DeviceType deviceType) {
        PractitionerDevice device = new PractitionerDevice();
        device.practitionerRole = role;
        device.fcmToken = fcmToken;
        device.deviceType = deviceType;
        device.isActive = true;
        LocalDateTime now = LocalDateTime.now();
        device.registeredAt = now;
        device.lastUsedAt = now;
        return device;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void touchLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

}