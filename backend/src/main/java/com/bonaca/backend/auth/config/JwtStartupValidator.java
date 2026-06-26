package com.bonaca.backend.auth.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtStartupValidator {

    private static final String SENTINEL = "CHANGE_ME_NOT_FOR_PROD";
    private static final java.util.Set<String> SAFE_PROFILES =
            java.util.Set.of("local", "dev", "test");

    private final JwtProperties properties;
    private final Environment environment;

    public JwtStartupValidator(JwtProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        String[] active = environment.getActiveProfiles();
        boolean isSafeProfile = active.length == 0
                || Arrays.stream(active).anyMatch(SAFE_PROFILES::contains);
        if (SENTINEL.equals(properties.secret()) && !isSafeProfile) {
            throw new IllegalStateException(
                    "JWT_SECRET env var must be set in non-local environments");
        }
    }
}
