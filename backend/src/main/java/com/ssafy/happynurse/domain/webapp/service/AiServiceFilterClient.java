package com.ssafy.happynurse.domain.webapp.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class AiServiceFilterClient {

    private static final String FILTER_PATH = "/api/symptom/filter";

    private final RestClient restClient;

    public AiServiceFilterClient(@Qualifier("aiClassificationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public FilterResponse filter(String symptomText) {
        try {
            FilterResponse resp = restClient.post()
                    .uri(FILTER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("symptom_text", symptomText))
                    .retrieve()
                    .body(FilterResponse.class);
            return resp == null ? FilterResponse.passthrough() : resp;
        } catch (Exception e) {
            log.warn("[filter] AI 호출 실패 → 정제 없이 원문 통과 (text length={})",
                    symptomText.length(), e);
            return FilterResponse.passthrough();
        }
    }

    public record FilterResponse(@JsonProperty("cleaned_text") String cleanedText) {
        public static FilterResponse passthrough() {
            return new FilterResponse(null);
        }
    }
}
