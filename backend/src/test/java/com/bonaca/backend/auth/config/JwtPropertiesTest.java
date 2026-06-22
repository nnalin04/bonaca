package com.bonaca.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Binds against the real src/main/resources/application.yml (no "test" profile active here, so
 * application-test.yml's overrides don't apply) — confirms the bonaca.jwt.* dev defaults that
 * CLAUDE.md and the rest of the auth feature assume actually bind onto JwtProperties.
 */
@SpringBootTest(classes = JwtPropertiesTest.Config.class)
class JwtPropertiesTest {

    @EnableConfigurationProperties(JwtProperties.class)
    static class Config {
    }

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void bindsTheDevDefaultsFromApplicationYml() {
        assertThat(jwtProperties.secret()).isNotBlank();
        assertThat(jwtProperties.accessTokenTtlMinutes()).isEqualTo(15);
        assertThat(jwtProperties.refreshTokenTtlDays()).isEqualTo(30);
    }
}
