package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.webapp.dto.EncounterContext;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AiServiceClassificationClient implements SymptomClassificationLlmClient {

    private static final String CLASSIFY_PATH = "/api/symptom/classify";

    private final RestClient restClient;

    public AiServiceClassificationClient(@Qualifier("aiClassificationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SymptomClassificationService.SymptomClassificationResult classify(
            String symptomText, EncounterContext context) {
        EncounterContext ctx = context != null ? context : EncounterContext.empty();
        Map<String, Object> body = new HashMap<>();
        body.put("symptom_text", symptomText);
        if (ctx.departmentCode() != null) body.put("department_code", ctx.departmentCode());
        if (ctx.surgeryName() != null) body.put("surgery_name", ctx.surgeryName());
        if (ctx.diseaseName() != null) body.put("disease_name", ctx.diseaseName());
        if (ctx.chiefComplaint() != null) body.put("chief_complaint", ctx.chiefComplaint());
        if (ctx.age() != null) body.put("age", ctx.age());
        if (ctx.gender() != null) body.put("gender", ctx.gender());
        if (ctx.podDays() != null) body.put("pod_days", ctx.podDays());

        try {
            ClassificationResponse response = restClient.post()
                    .uri(CLASSIFY_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ClassificationResponse.class);
            if (response == null || response.priority() == null) {
                log.warn("AI classification returned empty body for text: {}", symptomText);
                return fallback();
            }
            SymptomPriority priority = parsePriority(response.priority());
            BigDecimal confidence = response.confidence() != null
                    ? BigDecimal.valueOf(response.confidence())
                    : null;
            return new SymptomClassificationService.SymptomClassificationResult(priority, confidence);
        } catch (Exception e) {
            log.error("AI classification call failed (text='{}', dept='{}'): {}",
                    symptomText, ctx.departmentCode(), e.getMessage());
            return fallback();
        }
    }

    private static SymptomPriority parsePriority(String raw) {
        try {
            return SymptomPriority.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("AI returned unknown priority '{}', falling back to MEDIUM", raw);
            return SymptomPriority.MEDIUM;
        }
    }

    private static SymptomClassificationService.SymptomClassificationResult fallback() {
        return new SymptomClassificationService.SymptomClassificationResult(SymptomPriority.MEDIUM, null);
    }

    public record ClassificationResponse(String priority, String category, Double confidence) {}
}
