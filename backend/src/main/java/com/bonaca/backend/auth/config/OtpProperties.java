package com.bonaca.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.otp")
public record OtpProperties(
        int codeLength, long ttlMinutes, int maxVerifyAttempts, int maxRequestsPerHour) {
}
