package com.bonaca.backend.wearable.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.spike")
public record SpikeProperties(String apiKey, String webhookSecret, String baseUrl) {
    public SpikeProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.spikeapi.com";
        }
    }
}
