package com.ssafy.happynurse.global.exception;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    HttpStatus getStatus();
    String getMessage();
    String name();
}
