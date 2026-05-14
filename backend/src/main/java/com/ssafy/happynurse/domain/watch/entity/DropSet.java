package com.ssafy.happynurse.domain.watch.entity;

/**
 * 수액 세트 종류 — gtt/min ↔ mL/hr 환산용 드롭 팩터 (gtt/mL)
 */
public enum DropSet {
    SET_10(10),
    SET_15(15),
    SET_20(20),
    SET_60(60);

    private final int dropFactor;

    DropSet(int dropFactor) {
        this.dropFactor = dropFactor;
    }

    public int getDropFactor() {
        return dropFactor;
    }
}
