package com.bonaca.backend.metrics.exception;

public class InvalidMetricRangeException extends RuntimeException {
    public InvalidMetricRangeException(String message) {
        super(message);
    }
}
