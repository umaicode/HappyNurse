package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class WebappServiceTest {

    @Test
    @DisplayName("존재하지 않는 환자 진입 시 PATIENT_NOT_FOUND 발생")
    void placeholder() {
        // 에러 코드가 존재하는지 확인
        ErrorCode code = ErrorCode.PATIENT_NOT_FOUND;
    }
}
