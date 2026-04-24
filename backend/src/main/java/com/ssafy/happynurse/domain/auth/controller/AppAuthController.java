package com.ssafy.happynurse.domain.auth.controller;

import com.ssafy.happynurse.domain.auth.dto.AppLoginResponse;
import com.ssafy.happynurse.domain.auth.dto.AuthResult;
import com.ssafy.happynurse.domain.auth.dto.LoginRequest;
import com.ssafy.happynurse.domain.auth.service.AuthService;
import com.ssafy.happynurse.global.response.ApiResponse;
import com.ssafy.happynurse.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AppLoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResult result = authService.login(
                request.employeeNumber(),
                request.password(),
                httpRequest.getRemoteAddr(),
                request.organizationId(),
                request.wardId()
        );

        return ResponseEntity.ok(
                ApiResponse.ok("로그인에 성공했습니다.", AppLoginResponse.from(result)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logout(userDetails.getSessionId());

        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다.", null));
    }
}
