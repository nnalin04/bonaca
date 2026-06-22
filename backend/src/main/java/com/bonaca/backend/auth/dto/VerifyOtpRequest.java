package com.bonaca.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank
        @Pattern(regexp = "^\\+[1-9][0-9]{6,14}$", message = "phoneNumber must be in E.164 format, e.g. +919876543210")
        String phoneNumber,
        @NotBlank
        @Pattern(regexp = "^[0-9]{4,8}$", message = "code must be numeric")
        String code) {
}
