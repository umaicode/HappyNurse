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

    // Practitioner
    PRACTITIONER_NOT_FOUND(HttpStatus.NOT_FOUND, "의료진을 찾을 수 없습니다."),
    EMPLOYEE_NUMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사원번호를 찾을 수 없습니다."),
    PRACTITIONER_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 병동에서 활성화된 역할을 찾을 수 없습니다."),

    // Business (409)
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),

    // NFC
    NFC_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 NFC 토큰입니다."),

    // Patient
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "환자를 찾을 수 없습니다."),
    PATIENT_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 환자입니다."),
    ENCOUNTER_NOT_FOUND(HttpStatus.NOT_FOUND, "활성 입원 정보를 찾을 수 없습니다."),
    ENCOUNTER_NOT_IN_MY_WARD(HttpStatus.FORBIDDEN, "본인 병동의 입원이 아닙니다."),
    PATIENT_VERIFY_FAILED(HttpStatus.UNAUTHORIZED, "이름 또는 생년월일이 일치하지 않습니다."),

    // Symptom
    BUTTON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 증상 버튼입니다."),
    SYMPTOM_INPUT_INVALID(HttpStatus.BAD_REQUEST, "버튼 또는 직접 입력 중 하나만 선택해야 합니다."),
    PATIENT_ID_MISMATCH(HttpStatus.FORBIDDEN, "본인의 증상만 제출할 수 있습니다."),

    // Nursing Record
    NURSING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "간호 기록을 찾을 수 없습니다."),
    NURSING_RECORD_NOT_AUTHOR(HttpStatus.FORBIDDEN, "본인이 작성한 간호 기록만 수정할 수 있습니다."),

    // Medication Administration
    MEDICATION_ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "투약 기록을 찾을 수 없습니다."),
    MEDICATION_ADMIN_NOT_AUTHOR(HttpStatus.FORBIDDEN, "본인이 투약한 기록만 수정할 수 있습니다."),

    // Record State
    INVALID_RECORD_STATUS(HttpStatus.BAD_REQUEST, "현재 상태에서 처리할 수 없는 요청입니다."),

    // Medication Verification (NFC 흐름)
    NFC_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "스캔한 NFC 태그를 찾을 수 없습니다."),
    NFC_TAG_NOT_MEDICATION(HttpStatus.BAD_REQUEST, "약물 NFC 태그가 아닙니다."),
    NFC_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "NFC 페이로드 형식이 올바르지 않습니다."),
    MEDICATION_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "해당 환자의 처방이 아닙니다."),
    MEDICATION_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "처방 오더를 찾을 수 없습니다."),
    MEDICATION_ORDER_PATIENT_MISMATCH(HttpStatus.BAD_REQUEST, "처방 오더의 환자가 일치하지 않습니다."),
    MEDICATION_ALREADY_ADMINISTERED(HttpStatus.CONFLICT, "이미 투약 완료된(또는 활성 상태가 아닌) 처방입니다."),
    MEDICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약물을 찾을 수 없습니다."),

    // IV Infusion
    IV_INFUSION_NOT_FOUND(HttpStatus.NOT_FOUND, "수액 정보를 찾을 수 없습니다."),
    IV_RATE_INPUT_INVALID(HttpStatus.BAD_REQUEST, "주입 속도 입력이 올바르지 않습니다 (mL/hr 또는 gtt/min+patientType 중 하나만 채워야 합니다)."),
    IV_INVALID_STATE(HttpStatus.BAD_REQUEST, "현재 수액 상태에서 처리할 수 없는 요청입니다."),
    IV_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "해당 처방으로 진행 중인 수액이 이미 존재합니다. 먼저 종료해주세요.");

    private final HttpStatus status;
    private final String message;
}
