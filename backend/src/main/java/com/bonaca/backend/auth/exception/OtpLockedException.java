package com.bonaca.backend.auth.exception;

public class OtpLockedException extends RuntimeException {
    public OtpLockedException(String message) {
        super(message);
    }
}
