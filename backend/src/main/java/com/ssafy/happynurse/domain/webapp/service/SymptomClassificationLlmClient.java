package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.webapp.dto.EncounterContext;

public interface SymptomClassificationLlmClient {

    SymptomClassificationService.SymptomClassificationResult classify(
            String symptomText,
            EncounterContext context);
}
