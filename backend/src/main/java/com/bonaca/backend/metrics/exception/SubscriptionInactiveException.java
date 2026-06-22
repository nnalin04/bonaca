package com.bonaca.backend.metrics.exception;

public class SubscriptionInactiveException extends RuntimeException {
    public SubscriptionInactiveException(String message) {
        super(message);
    }
}
