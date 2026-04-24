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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Patient Webapp", description = "환자 웹앱 API")
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

    @Operation(
            summary = "환자 NFC 태깅 진입",
            description = "환자 팔찌의 NFC 태그를 스캔하면 태그 URL에 포함된 patientId로 환자 정보를 조회합니다. 비로그인 접근 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = NfcEntryResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "활성 입원 정보 없음")
    })
    @GetMapping("/api/nfc/patients/{patientId}/entry")
    public ResponseEntity<ApiResponse<NfcEntryResponse>> nfcEntry(
            @Parameter(description = "NFC 태그 URL에 포함된 환자 ID", example = "1", required = true)
            @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(webappService.getPatientEntry(patientId)));
    }

    @Operation(
            summary = "환자 본인 확인",
            description = "이름과 생년월일(yyMMdd) 6자리로 본인 확인을 수행합니다. 성공 시 JWT를 HttpOnly Cookie(ACCESS_TOKEN)로 발급합니다. 이후 API 호출 시 쿠키가 자동으로 전송됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "본인 확인 성공 — ACCESS_TOKEN 쿠키 발급",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PatientVerifyResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이름 또는 생년월일 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (birthDate 형식 불일치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환자를 찾을 수 없음")
    })
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
