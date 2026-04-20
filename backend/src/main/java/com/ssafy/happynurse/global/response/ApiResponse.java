package com.ssafy.happynurse.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.happynurse.global.exception.ResponseCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final String errorCode;
    private final T data;

    private ApiResponse(boolean success, String message, String errorCode, T data) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    public static ApiResponse<Void> fail(ResponseCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode.name(), null);
    }

    public static <T> ApiResponse<T> fail(ResponseCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode.name(), data);
    }
}