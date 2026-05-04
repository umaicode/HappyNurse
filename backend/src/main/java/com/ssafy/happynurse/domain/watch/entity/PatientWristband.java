package com.ssafy.happynurse.domain.watch.entity;

import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_wristband")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientWristband {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wristband_id")
    private Long wristbandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfc_tag_id", nullable = false)
    private NfcTag nfcTag;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}