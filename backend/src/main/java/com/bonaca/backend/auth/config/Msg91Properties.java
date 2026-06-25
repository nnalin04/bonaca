package com.bonaca.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.msg91")
public record Msg91Properties(String authKey, String templateId, String baseUrl) {

    public Msg91Properties {
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://control.msg91.com" : baseUrl;
    }
}
