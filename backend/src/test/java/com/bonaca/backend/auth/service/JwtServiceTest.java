package com.bonaca.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.auth.config.JwtProperties;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("a-test-secret-that-is-long-enough-for-hs384!!".repeat(2).getBytes());

    @Test
    void issuedTokenValidatesAndCarriesClaims() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 15, 30));
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueAccessToken(userId, "+919876543210");
        var claims = jwtService.validateAccessToken(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().userId()).isEqualTo(userId);
        assertThat(claims.get().phoneNumber()).isEqualTo("+919876543210");
    }

    @Test
    void expiredTokenFailsValidation() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, -1, 30));

        String token = jwtService.issueAccessToken(UUID.randomUUID(), "+919876543210");

        assertThat(jwtService.validateAccessToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithDifferentSecretFailsValidation() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 15, 30));
        String otherSecret =
                Base64.getEncoder().encodeToString("a-different-test-secret-also-long-enough!!".repeat(2).getBytes());
        JwtService verifier = new JwtService(new JwtProperties(otherSecret, 15, 30));

        String token = issuer.issueAccessToken(UUID.randomUUID(), "+919876543210");

        assertThat(verifier.validateAccessToken(token)).isEmpty();
    }

    @Test
    void malformedTokenFailsValidation() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 15, 30));

        assertThat(jwtService.validateAccessToken("not-a-jwt")).isEmpty();
    }
}
