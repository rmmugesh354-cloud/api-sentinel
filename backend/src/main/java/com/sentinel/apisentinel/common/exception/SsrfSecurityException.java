package com.sentinel.apisentinel.common.exception;

import org.springframework.http.HttpStatus;

public class SsrfSecurityException extends ApiException {
    public SsrfSecurityException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
