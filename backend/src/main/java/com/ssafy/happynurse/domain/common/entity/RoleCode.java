package com.ssafy.happynurse.domain.common.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.ssafy.happynurse.global.exception.CustomException;
import com.ssafy.happynurse.global.exception.ErrorCode;

public enum RoleCode {
    head_nurse, nurse, doctor, admin;

    @JsonCreator
    public static RoleCode from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return RoleCode.valueOf(value.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}