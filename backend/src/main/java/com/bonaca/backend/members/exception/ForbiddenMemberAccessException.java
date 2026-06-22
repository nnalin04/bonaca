package com.bonaca.backend.members.exception;

public class ForbiddenMemberAccessException extends RuntimeException {
    public ForbiddenMemberAccessException(String message) {
        super(message);
    }
}
