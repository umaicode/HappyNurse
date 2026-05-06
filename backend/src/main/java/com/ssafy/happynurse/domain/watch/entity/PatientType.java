package com.ssafy.happynurse.domain.watch.entity;

/**
 * 수액 주입 시 gtt/min → mL/hr 환산용 환자 타입
 */
public enum PatientType {
    ADULT(20),
    PEDIATRIC(60);

    private final int dropFactor;

    PatientType(int dropFactor) {
        this.dropFactor = dropFactor;
    }

    public int getDropFactor() {
        return dropFactor;
    }
}
