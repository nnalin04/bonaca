package com.bonaca.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonaca.proxy-security")
public record ProxySecurityProperties(boolean enabled, String secret) {

    public ProxySecurityProperties {
        secret = secret == null ? "" : secret;
        if (enabled && secret.isBlank()) {
            throw new IllegalArgumentException(
                    "bonaca.proxy-security.secret must be configured when proxy security is enabled");
        }
    }
}
