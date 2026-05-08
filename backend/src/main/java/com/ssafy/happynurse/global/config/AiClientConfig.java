package com.ssafy.happynurse.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiClientConfig {

    @Bean(name = "aiClassificationRestClient")
    public RestClient aiClassificationRestClient(
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ai.classification.timeout-ms:5000}") int timeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(buildRequestFactory(timeoutMs))
                .build();
    }

    private static SimpleClientHttpRequestFactory buildRequestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(timeoutMs).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(timeoutMs).toMillis());
        return factory;
    }
}
