package com.ssafy.happynurse.domain.watch.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medication")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_id")
    private Long medicationId;

    @Column(name = "drug_classification_code", length = 3)
    private String drugClassificationCode; // 의약품 분류번호

    @Column(name = "main_ingredient_code", length = 9)
    private String mainIngredientCode; // 주성분코드

    @Column(name = "product_code", nullable = false, length = 9)
    private String productCode; // 제품코드

    @Column(name = "product_name", length = 64)
    private String productName; // 제품명

    @Column(name = "manufacturer_name", length = 30)
    private String manufacturerName; // 업체명

    @Column(name = "atc_code", length = 10)
    private String atcCode;

    @Column(name = "atc_code_name", length = 100)
    private String atcCodeName; // ATC코드 명칭

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfc_tag_id")
    private NfcTag nfcTag;
}