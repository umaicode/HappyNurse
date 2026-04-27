package com.ssafy.happynurse.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ResponseCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

    // Auth (401, 403)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "사원번호 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),

    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "리프레시 토큰 재사용이 감지되었습니다. 다시 로그인해주세요."),

    // Organization / Ward
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 기관입니다."),
    WARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 병동입니다."),
    WARD_NOT_IN_ORGANIZATION(HttpStatus.BAD_REQUEST, "해당 기관에 속하지 않는 병동입니다."),

    // Account
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "퇴사 처리된 계정입니다."),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "해당 병동에 대한 권한이 없습니다."),

    // Business (409)
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),

    // Patient
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "환자를 찾을 수 없습니다."),
    PATIENT_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 환자입니다."),
    ENCOUNTER_NOT_FOUND(HttpStatus.NOT_FOUND, "활성 입원 정보를 찾을 수 없습니다."),
    PATIENT_VERIFY_FAILED(HttpStatus.UNAUTHORIZED, "이름 또는 생년월일이 일치하지 않습니다."),

    // Symptom
    BUTTON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 증상 버튼입니다."),
    SYMPTOM_INPUT_INVALID(HttpStatus.BAD_REQUEST, "버튼 또는 직접 입력 중 하나만 선택해야 합니다."),
    PATIENT_ID_MISMATCH(HttpStatus.FORBIDDEN, "본인의 증상만 제출할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
