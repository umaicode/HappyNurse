package com.ssafy.happynurse.domain.webapp.controller;

import com.ssafy.happynurse.domain.webapp.dto.*;
import com.ssafy.happynurse.domain.webapp.service.WebappService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CookieUtil;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

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
    @GetMapping("/nfc/patients/{patientId}/entry")
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
    @PostMapping("/patients/verify")
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
                        result.getRoomName(),
                        result.getGender(),
                        result.getDepartmentCode(),
                        result.getDiseaseName(),
                        result.getChiefComplaint(),
                        result.getSurgeryName(),
                        result.getAssignedNurseName()
                )
        ));
    }

    @Operation(
            summary = "증상 버튼 목록 조회",
            description = "환자가 선택할 수 있는 증상 버튼 목록을 반환합니다. 화면 1 본인 확인 후 발급된 환자 JWT가 필요합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "버튼 목록 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "환자 JWT 없음")
    })
    @GetMapping("/symptoms/buttons")
    public ResponseEntity<ApiResponse<List<SymptomButtonResponse>>> getButtons() {
        return ResponseEntity.ok(ApiResponse.ok(webappService.getButtons()));
    }

    @Operation(
            summary = "증상 제출",
            description = "환자가 증상 버튼을 선택하거나 직접 텍스트를 입력하여 증상을 제출합니다. " +
                    "제출 성공 시 같은 병동 간호사에게 실시간 SSE 알림이 전송됩니다. " +
                    "buttonId와 symptomText 중 하나만 입력해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "증상 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (버튼/텍스트 둘 다 있거나 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 아닌 환자 ID 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버튼 없음 또는 활성 입원 없음")
    })
    @PostMapping("/patients/{patientId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomSubmitResponse>> submitSymptom(
            @Parameter(description = "환자 ID (JWT의 subject와 일치해야 함)", example = "1", required = true)
            @PathVariable Long patientId,
            @RequestBody SymptomSubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                webappService.submitSymptom(userDetails.getPractitionerId(), patientId, request)
        ));
    }
}
