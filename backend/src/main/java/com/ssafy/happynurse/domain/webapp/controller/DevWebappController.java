package com.ssafy.happynurse.domain.webapp.controller;

import com.ssafy.happynurse.domain.webapp.dto.DevPatientVerifyResponse;
import com.ssafy.happynurse.domain.webapp.dto.DevVerifyRequest;
import com.ssafy.happynurse.domain.webapp.dto.PatientVerifyResult;
import com.ssafy.happynurse.domain.webapp.service.WebappService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "app.dev.enabled", havingValue = "true")
@Tag(name = "환자 웹앱", description = "[개발용] 개발 환경 전용 API")
@RestController
@RequiredArgsConstructor
@Slf4j
public class DevWebappController {

    private final WebappService webappService;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.access-token-expiration-ms}")
    private long expirationMs;

    @Value("${jwt.cookie-secure}")
    private boolean secure;

    @Value("${jwt.cookie-same-site}")
    private String sameSite;

    @PostConstruct
    void warnEnabled() {
        log.warn("==============================================================");
        log.warn("[DEV] /patients/dev-verify endpoint is ENABLED.");
        log.warn("This must NOT be enabled on production — disable APP_DEV_ENABLED.");
        log.warn("==============================================================");
    }

    @Operation(
            summary = "[개발용] 환자 본인 확인 (검증 생략)",
            description = "patientId만으로 환자 JWT를 발급합니다. " +
                    "app.dev.enabled=true일 때만 활성화됩니다. " +
                    "accessToken이 Response Body와 HttpOnly Cookie로 동시에 반환됩니다."
    )
    @PostMapping("/patients/dev-verify")
    public ResponseEntity<ApiResponse<DevPatientVerifyResponse>> devVerifyPatient(
            @RequestBody @Valid DevVerifyRequest request,
            HttpServletResponse response
    ) {
        PatientVerifyResult result = webappService.devVerify(request.patientId());

        ResponseCookie cookie = CookieUtil.createAccessTokenCookie(
                cookieName, result.getToken(), expirationMs, secure, sameSite);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(DevPatientVerifyResponse.from(result)));
    }
}