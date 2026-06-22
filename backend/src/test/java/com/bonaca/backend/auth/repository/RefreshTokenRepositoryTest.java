package com.bonaca.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.auth.model.RefreshToken;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Schema contract from V1__create_auth_schema.sql: token_hash is UNIQUE — RefreshTokenService's
 * rotate/revoke logic looks tokens up purely by hash, so a collision would let one user's
 * rotation accidentally affect another's session.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void findByTokenHashReturnsTheMatchingToken() {
        UUID userId = UUID.randomUUID();
        refreshTokenRepository.saveAndFlush(
                new RefreshToken(userId, "a-hash", Instant.now().plus(30, ChronoUnit.DAYS)));

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("a-hash");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByTokenHashReturnsEmptyForAnUnknownHash() {
        assertThat(refreshTokenRepository.findByTokenHash("unknown-hash")).isEmpty();
    }

    @Test
    void tokenHashMustBeUnique() {
        UUID userId = UUID.randomUUID();
        refreshTokenRepository.saveAndFlush(
                new RefreshToken(userId, "duplicate-hash", Instant.now().plus(30, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
                        new RefreshToken(userId, "duplicate-hash", Instant.now().plus(30, ChronoUnit.DAYS))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
