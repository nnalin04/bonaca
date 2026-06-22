package com.bonaca.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Binds against the real src/main/resources/application.yml. These numbers (4-digit code,
 * 5-minute TTL, 5 verify attempts, 5 requests/hour) aren't specified anywhere in docs/PRD.md or
 * docs/PRD.pdf — application.yml is the only documented contract for them, so this test is what
 * pins that contract rather than letting it drift silently.
 */
@SpringBootTest(classes = OtpPropertiesTest.Config.class)
class OtpPropertiesTest {

    @EnableConfigurationProperties(OtpProperties.class)
    static class Config {
    }

    @Autowired
    private OtpProperties otpProperties;

    @Test
    void bindsTheDevDefaultsFromApplicationYml() {
        assertThat(otpProperties.codeLength()).isEqualTo(4);
        assertThat(otpProperties.ttlMinutes()).isEqualTo(5);
        assertThat(otpProperties.maxVerifyAttempts()).isEqualTo(5);
        assertThat(otpProperties.maxRequestsPerHour()).isEqualTo(5);
    }
}
