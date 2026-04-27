package com.ssafy.happynurse.domain.common.entity;
import com.ssafy.happynurse.domain.patient.entity.Ward;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "practitioner_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PractitionerRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practitioner_role_id")
    private Long practitionerRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false)
    private RoleCode roleCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}