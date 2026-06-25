package com.bonaca.backend.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret, String planId) {}
