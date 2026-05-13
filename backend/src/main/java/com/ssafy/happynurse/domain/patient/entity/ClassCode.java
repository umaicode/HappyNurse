package com.ssafy.happynurse.domain.patient.entity;

/**
 * HL7 v3 ActCode 기반 진료 구분.
 * label 은 인수인계 등 UI 노출용 한국어 라벨.
 */
public enum ClassCode {
    AMB("외래"),
    EMER("응급"),
    IMP("입원"),
    ACUTE("급성 입원"),
    HH("가정 의료");

    private final String label;

    ClassCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}