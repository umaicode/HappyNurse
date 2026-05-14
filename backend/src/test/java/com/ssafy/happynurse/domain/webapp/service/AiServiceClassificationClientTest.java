package com.ssafy.happynurse.domain.webapp.service;

import com.ssafy.happynurse.domain.webapp.dto.EncounterContext;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AiServiceClassificationClientTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private AiServiceClassificationClient client;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.post()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(bodySpec);
        given(bodySpec.contentType(any(MediaType.class))).willReturn(bodySpec);
        given(bodySpec.body(any(Object.class))).willReturn(bodySpec);
        given(bodySpec.retrieve()).willReturn(responseSpec);

        client = new AiServiceClassificationClient(restClient);
    }

    @Test
    @DisplayName("정상 응답 — priority/confidence를 매핑해 반환한다")
    void classify_성공_정상_응답() {
        given(responseSpec.body(eq(AiServiceClassificationClient.ClassificationResponse.class)))
                .willReturn(new AiServiceClassificationClient.ClassificationResponse(
                        "high", "pain", 0.85));

        var result = client.classify("어깨가 아파요", EncounterContext.ofDepartment("OS"));

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
        assertThat(result.confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.85));
    }

    @Test
    @DisplayName("CRITICAL priority 문자열도 정확히 매핑된다")
    void classify_성공_CRITICAL_매핑() {
        given(responseSpec.body(eq(AiServiceClassificationClient.ClassificationResponse.class)))
                .willReturn(new AiServiceClassificationClient.ClassificationResponse(
                        "critical", "respiratory", 0.93));

        var result = client.classify("뭔가 이상해요", EncounterContext.ofDepartment("CS"));

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("HTTP 호출 실패 시 MEDIUM fallback")
    void classify_실패_HTTP_예외_MEDIUM_fallback() {
        given(responseSpec.body(eq(AiServiceClassificationClient.ClassificationResponse.class)))
                .willThrow(new ResourceAccessException("connection refused"));

        var result = client.classify("아무 텍스트", EncounterContext.ofDepartment("GS"));

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
        assertThat(result.confidence()).isNull();
    }

    @Test
    @DisplayName("응답 body가 null이면 MEDIUM fallback")
    void classify_실패_null_body_MEDIUM_fallback() {
        given(responseSpec.body(eq(AiServiceClassificationClient.ClassificationResponse.class)))
                .willReturn(null);

        var result = client.classify("아무 텍스트", EncounterContext.empty());

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("priority 필드가 알 수 없는 값이면 MEDIUM fallback")
    void classify_성공_알수없는_priority_MEDIUM_fallback() {
        given(responseSpec.body(eq(AiServiceClassificationClient.ClassificationResponse.class)))
                .willReturn(new AiServiceClassificationClient.ClassificationResponse(
                        "unknown_value", null, 0.5));

        var result = client.classify("텍스트", EncounterContext.empty());

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }
}
