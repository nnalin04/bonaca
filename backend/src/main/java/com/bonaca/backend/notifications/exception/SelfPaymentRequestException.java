package com.bonaca.backend.notifications.exception;

public class SelfPaymentRequestException extends RuntimeException {
    public SelfPaymentRequestException(String message) {
        super(message);
    }
}
