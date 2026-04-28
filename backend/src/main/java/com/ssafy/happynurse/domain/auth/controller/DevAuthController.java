package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.AppLoginResponse;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.DevLoginRequest;
import com.ssafy.happynurse.domain.auth.dto.SignupRequest;
import com.ssafy.happynurse.domain.auth.dto.SignupResponse;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ConditionalOnProperty(name = "app.dev.enabled", havingValue = "true")
@Tag(name = "인증 (DEV)", description = "[개발용] 테스트 계정 생성 및 로그인 API. app.dev.enabled=true 일 때만 활성화됩니다.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshExpirationMs;

    @PostConstruct
    void warnEnabled() {
        log.warn("==============================================================");
        log.warn("[DEV] /auth/signup, /auth/dev-login endpoints are ENABLED.");
        log.warn("This must NOT be enabled on production EC2 - disable APP_DEV_ENABLED.");
        log.warn("==============================================================");
    }

    @Operation(summary = "[DEV] 테스트 계정 생성",
            description = "Practitioner와 활성 PractitionerRole을 한 트랜잭션에서 생성합니다. 비밀번호는 BCrypt로 인코딩됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 / 잘못된 JSON - INVALID_INPUT_VALUE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "병동 없음 - WARD_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", description = "허용되지 않은 메서드 - METHOD_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "사원번호 중복 - DUPLICATE_RESOURCE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "지원하지 않는 Content-Type - UNSUPPORTED_MEDIA_TYPE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 - INTERNAL_SERVER_ERROR")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("계정이 생성되었습니다.", response));
    }

    @Operation(summary = "[DEV] 사원번호만으로 로그인",
            description = "사원번호만 입력하면 첫 번째 활성 역할 기준으로 토큰을 발급합니다. Swagger Authorize에 붙여 사용하세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 — 사원번호 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "활성 역할 없음(퇴사 처리됨) — ACCOUNT_DISABLED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사원번호 없음 — EMPLOYEE_NUMBER_NOT_FOUND")
    })
    @PostMapping("/dev-login")
    public ResponseEntity<ApiResponse<AppLoginResponse>> devLogin(
            @Valid @RequestBody DevLoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.devLogin(
                request.employeeNumber(),
                httpRequest.getRemoteAddr(),
                refreshExpirationMs);

        return ResponseEntity.ok(
                ApiResponse.ok("로그인에 성공했습니다.", AppLoginResponse.from(result)));
    }
}
