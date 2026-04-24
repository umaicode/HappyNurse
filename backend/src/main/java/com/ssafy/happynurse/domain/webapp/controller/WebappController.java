package com.ssafy.happynurse.domain.webapp.controller;

import com.ssafy.happynurse.domain.webapp.dto.*;
import com.ssafy.happynurse.domain.webapp.service.WebappService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WebappController {

    private final WebappService webappService;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.access-token-expiration-ms}")
    private long expirationMs;

    @Value("${jwt.cookie-secure}")
    private boolean secure;

    @Value("${jwt.cookie-same-site}")
    private String sameSite;

    @GetMapping("/api/nfc/patients/{patientId}/entry")
    public ResponseEntity<ApiResponse<NfcEntryResponse>> nfcEntry(
            @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(webappService.getPatientEntry(patientId)));
    }

    @PostMapping("/api/patients/verify")
    public ResponseEntity<ApiResponse<PatientVerifyResponse>> verifyPatient(
            @RequestBody @Valid PatientVerifyRequest request,
            HttpServletResponse response
    ) {
        PatientVerifyResult result = webappService.verifyPatient(request);

        ResponseCookie cookie = CookieUtil.createAccessTokenCookie(
                cookieName, result.getToken(), expirationMs, secure, sameSite);
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(
                new PatientVerifyResponse(
                        result.getPatientId(),
                        result.getPatientName(),
                        result.getRoomName()
                )
        ));
    }
}
