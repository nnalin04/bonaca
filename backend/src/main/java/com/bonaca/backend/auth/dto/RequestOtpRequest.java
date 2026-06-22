package com.bonaca.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RequestOtpRequest(
        @NotBlank
        @Pattern(regexp = "^\\+[1-9][0-9]{6,14}$", message = "phoneNumber must be in E.164 format, e.g. +919876543210")
        String phoneNumber) {
}
