package com.eliceteam8.edupay.global.exception;

import org.springframework.http.HttpStatus;

public class BillAlreadyPaidException extends RuntimeException {
    private final HttpStatus status;

    public BillAlreadyPaidException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}