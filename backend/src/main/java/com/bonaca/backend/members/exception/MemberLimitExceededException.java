package com.bonaca.backend.members.exception;

public class MemberLimitExceededException extends RuntimeException {
    public MemberLimitExceededException(String message) {
        super(message);
    }
}
