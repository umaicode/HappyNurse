package com.ssafy.happynurse.domain.webapp.service;

import org.springframework.lang.Nullable;

public interface SymptomClassificationLlmClient {

    SymptomClassificationService.SymptomClassificationResult classify(
            String symptomText,
            @Nullable String departmentCode);
}
