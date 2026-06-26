package com.bonaca.backend.common;

import com.bonaca.backend.auth.exception.InvalidOtpException;
import com.bonaca.backend.auth.exception.InvalidRefreshTokenException;
import com.bonaca.backend.auth.exception.OtpDeliveryException;
import com.bonaca.backend.auth.exception.OtpExpiredException;
import com.bonaca.backend.auth.exception.OtpLockedException;
import com.bonaca.backend.auth.exception.RateLimitExceededException;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberLimitExceededException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.metrics.exception.InvalidMetricRangeException;
import com.bonaca.backend.metrics.exception.InvalidMetricTypeException;
import com.bonaca.backend.metrics.exception.SubscriptionInactiveException;
import com.bonaca.backend.notifications.exception.NotificationNotFoundException;
import com.bonaca.backend.notifications.exception.SelfPaymentRequestException;
import com.bonaca.backend.subscriptions.exception.SubscriptionNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({InvalidOtpException.class, OtpExpiredException.class, OtpLockedException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(OtpDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleOtpDelivery(OtpDeliveryException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({MemberNotFoundException.class, SubscriptionNotFoundException.class, NotificationNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ForbiddenMemberAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenMemberAccess(ForbiddenMemberAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({MemberLimitExceededException.class, SelfPaymentRequestException.class, SubscriptionInactiveException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({InvalidMetricRangeException.class, InvalidMetricTypeException.class})
    public ResponseEntity<ErrorResponse> handleInvalidMetricRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Service not configured: " + ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIo(IOException ex) {
        log.error("External service I/O error", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("External service error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
    }

    public record ErrorResponse(String message) {
    }
}
